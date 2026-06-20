from sqlalchemy.orm import Session

from app.core.exception import InvalidCategoryException, PostNotFoundException
from app.domain.cms.enum import Section
from app.domain.pms.category_query_client import CategoryQueryClient, LocalCategoryQueryClient
from app.domain.pms.model import Post, PostCategory
from app.domain.pms.repository import PostCategoryRepository, PostRepository
from app.domain.pms.schema import CreatePostRequest, CreatePostResponse, PostAuthor, PostDetailResponse
from app.domain.pms.user_query_client import LocalUserQueryClient, UserQueryClient


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
