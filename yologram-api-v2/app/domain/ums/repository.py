from sqlalchemy.orm import Session

from app.domain.ums.model import User


class UserRepository:

    def __init__(self, db: Session):
        self.db = db

    def find_by_id(self, uid: int) -> User | None:
        return self.db.query(User).filter(User.id == uid).first()

    def find_by_email(self, email: str) -> User | None:
        return self.db.query(User).filter(User.email == email).first()

    def save(self, user: User) -> User:
        self.db.add(user)
        self.db.flush()
        self.db.refresh(user)
        return user
