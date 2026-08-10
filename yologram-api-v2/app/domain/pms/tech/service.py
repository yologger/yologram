from sqlalchemy.orm import Session

from app.core.exception import (
    InvalidPostCategoryException,
    InvalidSectionException,
    PostForbiddenException,
    PostNotFoundException,
)
from app.core.response import ApiEnvelopCursorPage, ApiEnvelopPage
from app.domain.pms.tech.cursor import TechPostCursor
from app.domain.pms.tech.model import TechPost, TechPostCategoryMapping, TechPostWithCommentCount
from app.domain.pms.tech.repository import TechPostCategoryMappingRepository, TechPostRepository
from app.domain.pms.tech.schema import (
    CreatePostRequest,
    CreatePostResponse,
    PostAuthor,
    PostDetailResponse,
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

MAX_PAGE_SIZE = 50
SECTION_PATH = "tech"


class TechPostService:

    def __init__(self, db: Session):
        self.post_repository = TechPostRepository(db)
        self.post_category_repository = TechPostCategoryMappingRepository(db)
        self.cms_api_client: CmsApiClient = LocalCmsApiClient(db)
        self.comment_api_client: CommentApiClient = LocalCommentApiClient(db)
        self.ums_api_client: UmsApiClient = LocalUmsApiClient(db)

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

    def get_post(self, id: int) -> PostDetailResponse:
        # 상세 단건 + 댓글 수 (tech_post_comment_count outerjoin+coalesce 프로젝션 — 없는 글이면 404)
        post_with_count = self.post_repository.find_post_with_comment_count(id)
        if post_with_count is None:
            raise PostNotFoundException()
        post = post_with_count.post

        category_ids = [pc.category_id for pc in self.post_category_repository.find_by_post_id(post.id)]
        nickname = self.ums_api_client.find_nickname(post.user_id)

        return PostDetailResponse(
            id=post.id,
            author=PostAuthor(uid=post.user_id, nickname=nickname),
            title=post.title,
            content=post.content,
            category_ids=category_ids,
            like_count=post.like_count,
            comment_count=post_with_count.comment_count,
            created_at=post.created_at,
        )

    # --- 피드 ---

    def get_posts_by_cursor(
        self, category_id: int | None, cursor: str | None, size: int
    ) -> ApiEnvelopCursorPage[PostSummaryResponse]:
        """테크 피드 (cursor 페이지네이션) — 실사용. id desc + keyset."""
        page_size = max(1, min(size, MAX_PAGE_SIZE))
        cursor_id = TechPostCursor.decode(cursor) if cursor else None

        posts = self.post_repository.find_posts(category_id, cursor_id, page_size)

        data = self._to_summaries(posts)
        next_cursor = TechPostCursor.encode(posts[-1].post.id) if posts else None
        return ApiEnvelopCursorPage(data=data, next_cursor=next_cursor)

    def get_posts_by_offset(
        self, category_id: int | None, page: int, size: int
    ) -> ApiEnvelopPage[PostSummaryResponse]:
        """테크 피드 (offset 페이지네이션) — 학습용. cursor 방식과 대비되는 offset+count 예시."""
        page_number = max(0, page)
        page_size = max(1, min(size, MAX_PAGE_SIZE))
        offset = page_number * page_size

        total_count = self.post_repository.count_posts(category_id)
        posts = self.post_repository.find_posts_offset(category_id, offset, page_size)

        data = self._to_summaries(posts)
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

        data = self._to_summaries(posts)
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

        data = self._to_summaries(posts)
        return self._to_page(data, page_number, page_size, total_count)

    @staticmethod
    def _validate_section(section_path: str | None) -> None:
        """section 쿼리 파라미터 검증 (URL 호환). 생략 또는 tech(대소문자 무관)만 허용."""
        if section_path is not None and section_path.lower() != SECTION_PATH:
            raise InvalidSectionException()

    def _to_summaries(self, posts: list[TechPostWithCommentCount]) -> list[PostSummaryResponse]:
        """글(+댓글 수 프로젝션) 목록 → PostSummaryResponse. 닉네임·카테고리 배치 조회(N+1 회피)를 공유."""
        nicknames = self.ums_api_client.find_nicknames([p.post.user_id for p in posts])

        category_ids_by_post: dict[int, list[int]] = {}
        for pc in self.post_category_repository.find_by_post_ids([p.post.id for p in posts]):
            category_ids_by_post.setdefault(pc.post_id, []).append(pc.category_id)

        return [
            PostSummaryResponse(
                id=post_with_count.post.id,
                author=PostAuthor(
                    uid=post_with_count.post.user_id,
                    nickname=nicknames.get(post_with_count.post.user_id),
                ),
                title=post_with_count.post.title,
                content=post_with_count.post.content,
                category_ids=category_ids_by_post.get(post_with_count.post.id, []),
                like_count=post_with_count.post.like_count,
                comment_count=post_with_count.comment_count,
                created_at=post_with_count.post.created_at,
            )
            for post_with_count in posts
        ]

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
