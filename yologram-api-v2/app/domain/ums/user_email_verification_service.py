import random
from datetime import datetime, timedelta

from sqlalchemy.orm import Session

from app.core.exception import (
    UserEmailVerificationExpiredException,
    UserEmailVerificationInvalidException,
    UserDuplicateException,
)
from app.domain.ums.email_sender import EmailSender
from app.domain.ums.model import UserEmailVerification
from app.domain.ums.repository import UserEmailVerificationRepository, UserRepository


class UserEmailVerificationService:

    def __init__(self, db: Session, email_sender: EmailSender):
        self.repository = UserEmailVerificationRepository(db)
        self.user_repository = UserRepository(db)
        self.email_sender = email_sender

    def send_code(self, email: str) -> None:
        if self.user_repository.find_by_email(email):
            raise UserDuplicateException()

        self.repository.delete_by_email(email)

        code = f"{random.randint(0, 999999):06d}"
        entity = UserEmailVerification(
            email=email,
            code=code,
            expired_at=datetime.now() + timedelta(minutes=5),
        )
        self.repository.save(entity)
        self.email_sender.send_verification_code(email, code)

    def verify_code(self, email: str, code: str) -> None:
        entity = self.repository.find_latest_by_email(email)
        if not entity:
            raise UserEmailVerificationInvalidException()
        if entity.expired_at < datetime.now():
            raise UserEmailVerificationExpiredException()
        if entity.code != code:
            raise UserEmailVerificationInvalidException()
        entity.verified = True
