from sqlalchemy.orm import Session

from app.domain.pms.model import Post, PostCategory


class PostRepository:

    def __init__(self, db: Session):
        self.db = db

    def save(self, post: Post) -> Post:
        self.db.add(post)
        self.db.flush()
        self.db.refresh(post)
        return post


class PostCategoryRepository:

    def __init__(self, db: Session):
        self.db = db

    def save(self, post_category: PostCategory) -> PostCategory:
        self.db.add(post_category)
        self.db.flush()
        return post_category
