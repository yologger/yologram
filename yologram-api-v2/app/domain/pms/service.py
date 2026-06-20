from sqlalchemy.orm import Session

from app.core.exception import InvalidCategoryException, PostNotFoundException
from app.core.response import ApiEnvelopCursorPage
from app.domain.cms.enum import Section
from app.domain.pms.category_query_client import CategoryQueryClient, LocalCategoryQueryClient
from app.domain.pms.cursor import PostCursor
from app.domain.pms.model import Post, PostCategory
from app.domain.pms.repository import PostCategoryRepository, PostRepository
from app.domain.pms.schema import (
    CreatePostRequest,
    CreatePostResponse,
    PostAuthor,
    PostDetailResponse,
    PostSummaryResponse,
)
from app.domain.pms.user_query_client import LocalUserQueryClient, UserQueryClient

MAX_PAGE_SIZE = 50


class PostService:

    def __init__(self, db: Session):
        self.post_repository = PostRepository(db)
        self.post_category_repository = PostCategoryRepository(db)
        self.category_query_client: CategoryQueryClient = LocalCategoryQueryClient(db)
        self.user_query_client: UserQueryClient = LocalUserQueryClient(db)

    def create(self, section_path: str, user_id: int, request: CreatePostRequest) -> CreatePostResponse:
        section = Section.from_path(section_path)
        category_ids = list(dict.fromkeys(request.category_ids))  # 중복 제거(순서 유지)

        if not self.category_query_client.all_active_in_section(section, category_ids):
            raise InvalidCategoryException()

        post = self.post_repository.save(
            Post(
                section=section,
                user_id=user_id,
                title=(request.title or None),
                content=request.content,
            )
        )

        for category_id in category_ids:
            self.post_category_repository.save(PostCategory(post_id=post.id, category_id=category_id))

        return CreatePostResponse(id=post.id)

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

    def get_posts(
        self, section_path: str, category_id: int | None, cursor: str | None, size: int
    ) -> ApiEnvelopCursorPage[PostSummaryResponse]:
        # 1) 섹션 검증
        section = Section.from_path(section_path)

        # 2) size 보정 (1~50)
        page_size = max(1, min(size, MAX_PAGE_SIZE))

        # 3) cursor 디코딩(마지막으로 본 글 id), 없으면 첫 페이지. 깨진 값이면 400 INVALID_CURSOR
        cursor_id = PostCursor.decode(cursor) if cursor else None

        # 4) 목록 조회 (id desc, cursor_id보다 과거 글)
        posts = self.post_repository.find_posts_by_section(section, category_id, cursor_id, page_size)

        # 5) 작성자 닉네임 배치 조회 (N+1 회피, ums 경계 추상화)
        nicknames = self.user_query_client.find_nicknames([p.user_id for p in posts])

        # 6) 카테고리 배치 조회 (N+1 회피, 1:N이라 join 대신 IN) → post_id별 그룹핑
        category_ids_by_post: dict[int, list[int]] = {}
        for pc in self.post_category_repository.find_by_post_ids([p.id for p in posts]):
            category_ids_by_post.setdefault(pc.post_id, []).append(pc.category_id)

        # 7) DTO 매핑
        data = [
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

        # 8) 마지막 글 id를 다음 커서로(빈 결과면 None). 클라이언트는 빈 응답으로 끝을 판단
        next_cursor = PostCursor.encode(posts[-1].id) if posts else None

        return ApiEnvelopCursorPage(data=data, next_cursor=next_cursor)
