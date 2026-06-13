import random
from datetime import datetime, timedelta

from sqlalchemy.orm import Session

from app.core.exception import (
    EmailVerificationExpiredException,
    EmailVerificationInvalidException,
    UserDuplicateException,
)
from app.domain.ums.email_sender import EmailSender
from app.domain.ums.model import EmailVerificationCode
from app.domain.ums.repository import EmailVerificationCodeRepository, UserRepository


class EmailVerificationService:

    def __init__(self, db: Session, email_sender: EmailSender):
        self.repository = EmailVerificationCodeRepository(db)
        self.user_repository = UserRepository(db)
        self.email_sender = email_sender

    def send_code(self, email: str) -> None:
        if self.user_repository.find_by_email(email):
            raise UserDuplicateException()

        self.repository.delete_by_email(email)

        code = f"{random.randint(0, 999999):06d}"
        entity = EmailVerificationCode(
            email=email,
            code=code,
            expired_at=datetime.now() + timedelta(minutes=5),
        )
        self.repository.save(entity)
        self.email_sender.send_verification_code(email, code)

    def verify_code(self, email: str, code: str) -> None:
        entity = self.repository.find_latest_by_email(email)
        if not entity:
            raise EmailVerificationInvalidException()
        if entity.expired_at < datetime.now():
            raise EmailVerificationExpiredException()
        if entity.code != code:
            raise EmailVerificationInvalidException()
        entity.verified = True
