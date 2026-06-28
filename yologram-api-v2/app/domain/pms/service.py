from sqlalchemy.orm import Session

from app.core.exception import InvalidPostCategoryException, PostForbiddenException, PostNotFoundException
from app.core.response import ApiEnvelopCursorPage, ApiEnvelopPage
from app.domain.cms.enum import Section
from app.domain.pms.post_category_query_client import PostCategoryQueryClient, LocalPostCategoryQueryClient
from app.domain.pms.cursor import PostCursor
from app.domain.pms.model import Post, PostCategoryMapping
from app.domain.pms.repository import PostCategoryMappingRepository, PostRepository
from app.domain.pms.schema import (
    CreatePostRequest,
    CreatePostResponse,
    PostAuthor,
    PostDetailResponse,
    PostSummaryResponse,
    UpdatePostRequest,
)
from app.domain.pms.user_query_client import LocalUserQueryClient, UserQueryClient

MAX_PAGE_SIZE = 50


class PostService:

    def __init__(self, db: Session):
        self.post_repository = PostRepository(db)
        self.post_category_repository = PostCategoryMappingRepository(db)
        self.post_category_query_client: PostCategoryQueryClient = LocalPostCategoryQueryClient(db)
        self.user_query_client: UserQueryClient = LocalUserQueryClient(db)

    def create(self, section_path: str, user_id: int, request: CreatePostRequest) -> CreatePostResponse:
        section = Section.from_path(section_path)
        category_ids = list(dict.fromkeys(request.category_ids))  # 중복 제거(순서 유지)

        if not self.post_category_query_client.all_active_in_section(section, category_ids):
            raise InvalidPostCategoryException()

        post = self.post_repository.save(
            Post(
                section=section,
                user_id=user_id,
                title=(request.title or None),
                content=request.content,
            )
        )

        for category_id in category_ids:
            self.post_category_repository.save(PostCategoryMapping(post_id=post.id, category_id=category_id))

        return CreatePostResponse(id=post.id)

    def update(self, section_path: str, post_id: int, user_id: int, request: UpdatePostRequest) -> None:
        """게시글 수정 (본인 글). 제목·내용 갱신 + 카테고리 매핑 전체 교체."""
        section = Section.from_path(section_path)

        # 없거나 다른 section의 글이면 404 (상세 조회와 동일 규칙)
        post = self.post_repository.find_by_id(post_id)
        if post is None or post.section != section:
            raise PostNotFoundException()

        # 작성자 본인만 수정 가능 (아니면 403)
        if post.user_id != user_id:
            raise PostForbiddenException()

        # 카테고리 검증 (작성과 동일: 해당 section 활성 카테고리 1~3개)
        category_ids = list(dict.fromkeys(request.category_ids))  # 중복 제거(순서 유지)
        if not self.post_category_query_client.all_active_in_section(section, category_ids):
            raise InvalidPostCategoryException()

        # 제목·내용 갱신 (속성 변경 → get_db commit 시 flush, modified_date는 onupdate로 자동 갱신)
        post.title = request.title or None
        post.content = request.content

        # 카테고리 매핑은 전체 교체 (기존 제거 후 재생성)
        self.post_category_repository.delete_by_post_id(post.id)
        for category_id in category_ids:
            self.post_category_repository.save(PostCategoryMapping(post_id=post.id, category_id=category_id))

    def get_post(self, section_path: str, id: int) -> PostDetailResponse:
        section = Section.from_path(section_path)
        post = self.post_repository.find_by_id(id)
        if post is None or post.section != section:
            raise PostNotFoundException()

        category_ids = [pc.category_id for pc in self.post_category_repository.find_by_post_id(post.id)]
        nickname = self.user_query_client.find_nickname(post.user_id)

        return PostDetailResponse(
            id=post.id,
            section=post.section,
            author=PostAuthor(uid=post.user_id, nickname=nickname),
            title=post.title,
            content=post.content,
            category_ids=category_ids,
            like_count=post.like_count,
            comment_count=post.comment_count,
            created_at=post.created_at,
        )

    # --- 섹션 피드 ---

    def get_posts_by_cursor(
        self, section_path: str, category_id: int | None, cursor: str | None, size: int
    ) -> ApiEnvelopCursorPage[PostSummaryResponse]:
        """섹션 피드 (cursor 페이지네이션) — 실사용. id desc + keyset."""
        section = Section.from_path(section_path)
        page_size = max(1, min(size, MAX_PAGE_SIZE))
        cursor_id = PostCursor.decode(cursor) if cursor else None

        posts = self.post_repository.find_posts_by_section(section, category_id, cursor_id, page_size)

        data = self._to_summaries(posts)
        next_cursor = PostCursor.encode(posts[-1].id) if posts else None
        return ApiEnvelopCursorPage(data=data, next_cursor=next_cursor)

    def get_posts_by_offset(
        self, section_path: str, category_id: int | None, page: int, size: int
    ) -> ApiEnvelopPage[PostSummaryResponse]:
        """섹션 피드 (offset 페이지네이션) — 학습용. cursor 방식과 대비되는 offset+count 예시."""
        section = Section.from_path(section_path)
        page_number = max(0, page)
        page_size = max(1, min(size, MAX_PAGE_SIZE))
        offset = page_number * page_size

        total_count = self.post_repository.count_posts_by_section(section, category_id)
        posts = self.post_repository.find_posts_by_section_offset(section, category_id, offset, page_size)

        data = self._to_summaries(posts)
        return self._to_page(data, page_number, page_size, total_count)

    # --- 내 글 ---

    def get_my_posts_by_cursor(
        self, user_id: int, section_path: str | None, cursor: str | None, size: int
    ) -> ApiEnvelopCursorPage[PostSummaryResponse]:
        """내 글 목록 (cursor 페이지네이션) — 실사용. 피드와 동일 방식, 무한스크롤 적합."""
        section = Section.from_path(section_path) if section_path else None
        page_size = max(1, min(size, MAX_PAGE_SIZE))
        cursor_id = PostCursor.decode(cursor) if cursor else None

        posts = self.post_repository.find_my_posts_by_cursor(user_id, section, cursor_id, page_size)

        data = self._to_summaries(posts)
        next_cursor = PostCursor.encode(posts[-1].id) if posts else None
        return ApiEnvelopCursorPage(data=data, next_cursor=next_cursor)

    def get_my_posts_by_offset(
        self, user_id: int, section_path: str | None, page: int, size: int
    ) -> ApiEnvelopPage[PostSummaryResponse]:
        """내 글 목록 (offset 페이지네이션) — 학습용. cursor 방식과 대비되는 offset+count 예시."""
        section = Section.from_path(section_path) if section_path else None
        page_number = max(0, page)
        page_size = max(1, min(size, MAX_PAGE_SIZE))
        offset = page_number * page_size

        total_count = self.post_repository.count_my_posts(user_id, section)
        posts = self.post_repository.find_my_posts_by_offset(user_id, section, offset, page_size)

        data = self._to_summaries(posts)
        return self._to_page(data, page_number, page_size, total_count)

    def _to_summaries(self, posts: list[Post]) -> list[PostSummaryResponse]:
        """글 목록 → PostSummaryResponse. 닉네임·카테고리 배치 조회(N+1 회피)를 공유."""
        nicknames = self.user_query_client.find_nicknames([p.user_id for p in posts])

        category_ids_by_post: dict[int, list[int]] = {}
        for pc in self.post_category_repository.find_by_post_ids([p.id for p in posts]):
            category_ids_by_post.setdefault(pc.post_id, []).append(pc.category_id)

        return [
            PostSummaryResponse(
                id=post.id,
                section=post.section,
                author=PostAuthor(uid=post.user_id, nickname=nicknames.get(post.user_id)),
                title=post.title,
                content=post.content,
                category_ids=category_ids_by_post.get(post.id, []),
                like_count=post.like_count,
                comment_count=post.comment_count,
                created_at=post.created_at,
            )
            for post in posts
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
