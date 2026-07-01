from sqlalchemy.orm import Session

from app.domain.comment.model import Comment


class CommentRepository:

    def __init__(self, db: Session):
        self.db = db

    def save(self, comment: Comment) -> Comment:
        self.db.add(comment)
        self.db.flush()
        self.db.refresh(comment)
        return comment
