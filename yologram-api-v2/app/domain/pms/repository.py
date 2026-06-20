from sqlalchemy import exists
from sqlalchemy.orm import Session

from app.domain.cms.enum import Section
from app.domain.pms.model import Post, PostCategoryMapping


class PostRepository:

    def __init__(self, db: Session):
        self.db = db

    def save(self, post: Post) -> Post:
        self.db.add(post)
        self.db.flush()
        self.db.refresh(post)
        return post

    def find_by_id(self, id: int) -> Post | None:
        return self.db.query(Post).filter(Post.id == id).first()

    def find_posts_by_section(
        self, section: Section, category_id: int | None, cursor_id: int | None, limit: int
    ) -> list[Post]:
        """섹션 피드 (id desc), keyset 페이지네이션. cursor_id보다 과거 글부터 limit개."""
        query = self.db.query(Post).filter(Post.section == section)

        # 카테고리 필터(선택): EXISTS — 매칭 1건에 단축, 1:N에서도 행이 불어나지 않음
        if category_id is not None:
            query = query.filter(
                exists().where(
                    (PostCategoryMapping.post_id == Post.id) & (PostCategoryMapping.category_id == category_id)
                )
            )

        # 커서(선택): id가 곧 작성순이므로 id < cursor_id면 더 과거 글
        if cursor_id is not None:
            query = query.filter(Post.id < cursor_id)

        return query.order_by(Post.id.desc()).limit(limit).all()


class PostCategoryMappingRepository:

    def __init__(self, db: Session):
        self.db = db

    def save(self, post_category: PostCategoryMapping) -> PostCategoryMapping:
        self.db.add(post_category)
        self.db.flush()
        return post_category

    def find_by_post_id(self, post_id: int) -> list[PostCategoryMapping]:
        return self.db.query(PostCategoryMapping).filter(PostCategoryMapping.post_id == post_id).all()

    def find_by_post_ids(self, post_ids: list[int]) -> list[PostCategoryMapping]:
        if not post_ids:
            return []
        return self.db.query(PostCategoryMapping).filter(PostCategoryMapping.post_id.in_(post_ids)).all()
