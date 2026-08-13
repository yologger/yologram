from sqlalchemy.orm import Session

from app.core.exception import (
    InvalidPostCategoryException,
    InvalidSectionException,
    PostForbiddenException,
    PostNotFoundException,
)
from app.core.response import ApiEnvelopCursorPage, ApiEnvelopPage
from app.domain.pms.tech.cursor import TechPostCursor
from app.domain.pms.tech.event import PostViewEvent
from app.domain.pms.tech.model import TechPost, TechPostCategoryMapping, TechPostWithCounts
from app.domain.pms.tech.repository import (
    TechPostCategoryMappingRepository,
    TechPostLikeRepository,
    TechPostRepository,
)
from app.domain.pms.tech.schema import (
    CreatePostRequest,
    CreatePostResponse,
    PostAuthor,
    PostDetailResponse,
    PostMetrics,
    PostSummaryResponse,
    UpdatePostRequest,
)
from app.infra.client.cms.cms_api_client import (
    LocalCmsApiClient,
    CmsApiClient,
)
from app.infra.client.comment.comment_api_client import (
    LocalCommentApiClient,
    CommentApiClient,
)
from app.infra.client.ums.ums_api_client import LocalUmsApiClient, UmsApiClient
from app.infra.event.post_view_event_publisher import (
    KinesisPostViewEventPublisher,
    PostViewEventPublisher,
)

MAX_PAGE_SIZE = 50
SECTION_PATH = "tech"


class TechPostService:

    def __init__(self, db: Session):
        self.post_repository = TechPostRepository(db)
        self.post_category_repository = TechPostCategoryMappingRepository(db)
        self.like_repository = TechPostLikeRepository(db)
        self.cms_api_client: CmsApiClient = LocalCmsApiClient(db)
        self.comment_api_client: CommentApiClient = LocalCommentApiClient(db)
        self.ums_api_client: UmsApiClient = LocalUmsApiClient(db)
        # 조회 이벤트 발행 — 서비스는 Protocol만 알고 boto3는 infra 구현에 격리 (경계)
        self.post_view_event_publisher: PostViewEventPublisher = KinesisPostViewEventPublisher()

    def create(self, user_id: int, request: CreatePostRequest) -> CreatePostResponse:
        category_ids = list(dict.fromkeys(request.category_ids))  # 중복 제거(순서 유지)

        if not self.cms_api_client.all_active(category_ids):
            raise InvalidPostCategoryException()

        post = self.post_repository.save(
            TechPost(
                user_id=user_id,
                title=(request.title or None),
                content=request.content,
            )
        )

        for category_id in category_ids:
            self.post_category_repository.save(TechPostCategoryMapping(post_id=post.id, category_id=category_id))

        return CreatePostResponse(id=post.id)

    def update(self, post_id: int, user_id: int, request: UpdatePostRequest) -> None:
        """게시글 수정 (본인 글). 제목·내용 갱신 + 카테고리 매핑 전체 교체."""
        # 없으면 404 (상세 조회와 동일 규칙)
        post = self.post_repository.find_by_id(post_id)
        if post is None:
            raise PostNotFoundException()

        # 작성자 본인만 수정 가능 (아니면 403)
        if post.user_id != user_id:
            raise PostForbiddenException()

        # 카테고리 검증 (작성과 동일: 테크 게시판 활성 카테고리 1~3개)
        category_ids = list(dict.fromkeys(request.category_ids))  # 중복 제거(순서 유지)
        if not self.cms_api_client.all_active(category_ids):
            raise InvalidPostCategoryException()

        # 제목·내용 갱신 (속성 변경 → get_db commit 시 flush, modified_date는 onupdate로 자동 갱신)
        post.title = request.title or None
        post.content = request.content

        # 카테고리 매핑은 전체 교체 (기존 제거 후 재생성)
        self.post_category_repository.delete_by_post_id(post.id)
        for category_id in category_ids:
            self.post_category_repository.save(TechPostCategoryMapping(post_id=post.id, category_id=category_id))

    def delete(self, post_id: int, user_id: int) -> None:
        """게시글 삭제 (본인 글). 연관 데이터(카테고리 매핑·댓글) 정리 후 게시글 삭제 (한 트랜잭션)."""
        # 없으면 404 (상세 조회와 동일 규칙)
        post = self.post_repository.find_by_id(post_id)
        if post is None:
            raise PostNotFoundException()

        # 작성자 본인만 삭제 가능 (아니면 403)
        if post.user_id != user_id:
            raise PostForbiddenException()

        # 연관 데이터 정리 후 게시글 삭제 — 카테고리 매핑 + 댓글(고아 방지, CommentApiClient로 경계 추상화).
        # get_db 세션(요청 단위 commit)이라 글·매핑·댓글 삭제가 원자적 (좋아요 도메인은 미구현)
        self.post_category_repository.delete_by_post_id(post.id)
        self.comment_api_client.delete_by_post_id(post.id)
        self.post_repository.delete(post)

    def get_post(self, id: int, viewer_uid: int | None = None, client_ip: str | None = None) -> PostDetailResponse:
        """게시글 단건 조회. viewer_uid는 선택 인증 (로그인 시 metrics.likedByMe 계산, 비로그인 None → False).
        client_ip는 조회 이벤트용 (router가 X-Forwarded-For에서 추출해 전달, 없으면 None)."""
        # 상세 단건 + 카운트 (tech_post_comment_count·tech_post_like_count outerjoin+coalesce 프로젝션 — 없는 글이면 404)
        post_with_counts = self.post_repository.find_post_with_counts(id)
        if post_with_counts is None:
            raise PostNotFoundException()
        post = post_with_counts.post

        category_ids = [pc.category_id for pc in self.post_category_repository.find_by_post_id(post.id)]
        nickname = self.ums_api_client.find_nickname(post.user_id)

        # likedByMe: 개인화 값이라 프로젝션이 아닌 이력 단건 exists (비로그인은 조회 생략)
        liked_by_me = viewer_uid is not None and self.like_repository.exists_by_post_id_and_uid(post.id, viewer_uid)

        # 조회 이벤트 발행 — 조회가 성공한 뒤에만(404면 위에서 예외로 빠져 발행되지 않는다).
        # 실패는 publisher가 삼킨다(부가 기능이므로 조회 API 가용성 우선). 중복 판정은 소비 쪽(worker) 몫
        self.post_view_event_publisher.publish(PostViewEvent(post_id=post.id, uid=viewer_uid, ip=client_ip))

        return PostDetailResponse(
            id=post.id,
            author=PostAuthor(uid=post.user_id, nickname=nickname),
            title=post.title,
            content=post.content,
            category_ids=category_ids,
            metrics=self._to_metrics(post_with_counts, liked_by_me),
            created_at=post.created_at,
        )

    # --- 피드 ---

    def get_posts_by_cursor(
        self, category_id: int | None, cursor: str | None, size: int, viewer_uid: int | None = None
    ) -> ApiEnvelopCursorPage[PostSummaryResponse]:
        """테크 피드 (cursor 페이지네이션) — 실사용. id desc + keyset. viewer_uid는 선택 인증(likedByMe)."""
        page_size = max(1, min(size, MAX_PAGE_SIZE))
        cursor_id = TechPostCursor.decode(cursor) if cursor else None

        posts = self.post_repository.find_posts(category_id, cursor_id, page_size)

        data = self._to_summaries(posts, viewer_uid)
        next_cursor = TechPostCursor.encode(posts[-1].post.id) if posts else None
        return ApiEnvelopCursorPage(data=data, next_cursor=next_cursor)

    def get_posts_by_offset(
        self, category_id: int | None, page: int, size: int, viewer_uid: int | None = None
    ) -> ApiEnvelopPage[PostSummaryResponse]:
        """테크 피드 (offset 페이지네이션) — 학습용. cursor 방식과 대비되는 offset+count 예시."""
        page_number = max(0, page)
        page_size = max(1, min(size, MAX_PAGE_SIZE))
        offset = page_number * page_size

        total_count = self.post_repository.count_posts(category_id)
        posts = self.post_repository.find_posts_offset(category_id, offset, page_size)

        data = self._to_summaries(posts, viewer_uid)
        return self._to_page(data, page_number, page_size, total_count)

    # --- 내 글 ---

    def get_my_posts_by_cursor(
        self, user_id: int, section_path: str | None, cursor: str | None, size: int
    ) -> ApiEnvelopCursorPage[PostSummaryResponse]:
        """내 글 목록 (cursor 페이지네이션) — 실사용. 피드와 동일 방식, 무한스크롤 적합.
        section 쿼리 파라미터는 URL 호환용 — 생략 또는 tech만 허용(테이블 분리로 다른 섹션은 없음)."""
        self._validate_section(section_path)
        page_size = max(1, min(size, MAX_PAGE_SIZE))
        cursor_id = TechPostCursor.decode(cursor) if cursor else None

        posts = self.post_repository.find_my_posts_by_cursor(user_id, cursor_id, page_size)

        # viewer = 본인(인증 필수)이라 likedByMe도 user_id 기준
        data = self._to_summaries(posts, viewer_uid=user_id)
        next_cursor = TechPostCursor.encode(posts[-1].post.id) if posts else None
        return ApiEnvelopCursorPage(data=data, next_cursor=next_cursor)

    def get_my_posts_by_offset(
        self, user_id: int, section_path: str | None, page: int, size: int
    ) -> ApiEnvelopPage[PostSummaryResponse]:
        """내 글 목록 (offset 페이지네이션) — 학습용. cursor 방식과 대비되는 offset+count 예시."""
        self._validate_section(section_path)
        page_number = max(0, page)
        page_size = max(1, min(size, MAX_PAGE_SIZE))
        offset = page_number * page_size

        total_count = self.post_repository.count_my_posts(user_id)
        posts = self.post_repository.find_my_posts_by_offset(user_id, offset, page_size)

        data = self._to_summaries(posts, viewer_uid=user_id)
        return self._to_page(data, page_number, page_size, total_count)

    @staticmethod
    def _validate_section(section_path: str | None) -> None:
        """section 쿼리 파라미터 검증 (URL 호환). 생략 또는 tech(대소문자 무관)만 허용."""
        if section_path is not None and section_path.lower() != SECTION_PATH:
            raise InvalidSectionException()

    def _to_summaries(
        self, posts: list[TechPostWithCounts], viewer_uid: int | None = None
    ) -> list[PostSummaryResponse]:
        """글(+카운트 프로젝션) 목록 → PostSummaryResponse.
        닉네임·카테고리·likedByMe 배치 조회(N+1 회피)를 공유."""
        nicknames = self.ums_api_client.find_nicknames([p.post.user_id for p in posts])

        category_ids_by_post: dict[int, list[int]] = {}
        for pc in self.post_category_repository.find_by_post_ids([p.post.id for p in posts]):
            category_ids_by_post.setdefault(pc.post_id, []).append(pc.category_id)

        # likedByMe 배치: 로그인 유저가 누른 글만 이력 IN 1번 질의로 Set 구성 (비로그인·빈 목록은 생략)
        liked_post_ids: set[int] = set()
        if viewer_uid is not None and posts:
            liked_post_ids = self.like_repository.find_liked_post_ids(viewer_uid, [p.post.id for p in posts])

        return [
            PostSummaryResponse(
                id=post_with_counts.post.id,
                author=PostAuthor(
                    uid=post_with_counts.post.user_id,
                    nickname=nicknames.get(post_with_counts.post.user_id),
                ),
                title=post_with_counts.post.title,
                content=post_with_counts.post.content,
                category_ids=category_ids_by_post.get(post_with_counts.post.id, []),
                metrics=self._to_metrics(post_with_counts, post_with_counts.post.id in liked_post_ids),
                created_at=post_with_counts.post.created_at,
            )
            for post_with_counts in posts
        ]

    @staticmethod
    def _to_metrics(post_with_counts: TechPostWithCounts, liked_by_me: bool) -> PostMetrics:
        """프로젝션 카운트 + likedByMe → metrics 응답 변환 (목록·상세 공유)."""
        return PostMetrics(
            comment_count=post_with_counts.comment_count,
            like_count=post_with_counts.like_count,
            liked_by_me=liked_by_me,
        )

    @staticmethod
    def _to_page(
        data: list[PostSummaryResponse], page_number: int, page_size: int, total_count: int
    ) -> ApiEnvelopPage[PostSummaryResponse]:
        total_pages = 0 if total_count == 0 else (total_count + page_size - 1) // page_size
        return ApiEnvelopPage(
            data=data,
            page=page_number,
            size=page_size,
            total_pages=total_pages,
            total_count=total_count,
            first=(page_number == 0),
            last=(total_pages == 0 or page_number >= total_pages - 1),
        )
