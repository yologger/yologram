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

    def find_by_id(self, id: int) -> Post | None:
        return self.db.query(Post).filter(Post.id == id).first()


class PostCategoryRepository:

    def __init__(self, db: Session):
        self.db = db

    def save(self, post_category: PostCategory) -> PostCategory:
        self.db.add(post_category)
        self.db.flush()
        return post_category

    def find_by_post_id(self, post_id: int) -> list[PostCategory]:
        return self.db.query(PostCategory).filter(PostCategory.post_id == post_id).all()
