from sqlalchemy.orm import Session

from app.domain.ums.model import UserEmailVerification, UserPasswordResetCode, User


class UserRepository:

    def __init__(self, db: Session):
        self.db = db

    def find_by_id(self, uid: int) -> User | None:
        return self.db.query(User).filter(User.id == uid).first()

    def find_by_ids(self, uids: list[int]) -> list[User]:
        if not uids:
            return []
        return self.db.query(User).filter(User.id.in_(uids)).all()

    def find_by_email(self, email: str) -> User | None:
        return self.db.query(User).filter(User.email == email).first()

    def save(self, user: User) -> User:
        self.db.add(user)
        self.db.flush()
        self.db.refresh(user)
        return user

    def delete(self, user: User) -> None:
        self.db.delete(user)
        self.db.flush()


class UserEmailVerificationRepository:

    def __init__(self, db: Session):
        self.db = db

    def find_latest_by_email(self, email: str) -> UserEmailVerification | None:
        return self.db.query(UserEmailVerification).filter(
            UserEmailVerification.email == email,
        ).order_by(UserEmailVerification.created_at.desc()).first()

    def save(self, entity: UserEmailVerification) -> UserEmailVerification:
        self.db.add(entity)
        self.db.flush()
        self.db.refresh(entity)
        return entity

    def delete_by_email(self, email: str) -> None:
        self.db.query(UserEmailVerification).filter(
            UserEmailVerification.email == email,
        ).delete()


class UserPasswordResetCodeRepository:

    def __init__(self, db: Session):
        self.db = db

    def find_latest_by_email(self, email: str) -> UserPasswordResetCode | None:
        return self.db.query(UserPasswordResetCode).filter(
            UserPasswordResetCode.email == email,
        ).order_by(UserPasswordResetCode.created_at.desc()).first()

    def save(self, entity: UserPasswordResetCode) -> UserPasswordResetCode:
        self.db.add(entity)
        self.db.flush()
        self.db.refresh(entity)
        return entity

    def delete_by_email(self, email: str) -> None:
        self.db.query(UserPasswordResetCode).filter(
            UserPasswordResetCode.email == email,
        ).delete()
