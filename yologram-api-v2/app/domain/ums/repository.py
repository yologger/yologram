from sqlalchemy.orm import Session

from app.domain.ums.model import EmailVerificationCode, PasswordResetCode, User


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


class EmailVerificationCodeRepository:

    def __init__(self, db: Session):
        self.db = db

    def find_latest_by_email(self, email: str) -> EmailVerificationCode | None:
        return self.db.query(EmailVerificationCode).filter(
            EmailVerificationCode.email == email,
        ).order_by(EmailVerificationCode.created_at.desc()).first()

    def save(self, entity: EmailVerificationCode) -> EmailVerificationCode:
        self.db.add(entity)
        self.db.flush()
        self.db.refresh(entity)
        return entity

    def delete_by_email(self, email: str) -> None:
        self.db.query(EmailVerificationCode).filter(
            EmailVerificationCode.email == email,
        ).delete()


class PasswordResetCodeRepository:

    def __init__(self, db: Session):
        self.db = db

    def find_latest_by_email(self, email: str) -> PasswordResetCode | None:
        return self.db.query(PasswordResetCode).filter(
            PasswordResetCode.email == email,
        ).order_by(PasswordResetCode.created_at.desc()).first()

    def save(self, entity: PasswordResetCode) -> PasswordResetCode:
        self.db.add(entity)
        self.db.flush()
        self.db.refresh(entity)
        return entity

    def delete_by_email(self, email: str) -> None:
        self.db.query(PasswordResetCode).filter(
            PasswordResetCode.email == email,
        ).delete()
