import random
from datetime import datetime, timedelta

import bcrypt
from sqlalchemy.orm import Session

from app.core.exception import (
    UserPasswordResetExpiredException,
    UserPasswordResetInvalidException,
    UserNotFoundException,
)
from app.domain.ums.email_sender import EmailSender
from app.domain.ums.model import UserPasswordResetCode
from app.domain.ums.repository import UserPasswordResetCodeRepository, UserRepository


class UserPasswordResetService:

    def __init__(self, db: Session, email_sender: EmailSender):
        self.repository = UserPasswordResetCodeRepository(db)
        self.user_repository = UserRepository(db)
        self.email_sender = email_sender

    def send_code(self, email: str) -> None:
        if not self.user_repository.find_by_email(email):
            raise UserNotFoundException()

        self.repository.delete_by_email(email)

        code = f"{random.randint(0, 999999):06d}"
        entity = UserPasswordResetCode(
            email=email,
            code=code,
            expired_at=datetime.now() + timedelta(minutes=5),
        )
        self.repository.save(entity)
        self.email_sender.send_password_reset_code(email, code)

    def verify_code(self, email: str, code: str) -> None:
        entity = self._find_valid_code(email, code)
        entity.verified = True

    def confirm(self, email: str, code: str, new_password: str) -> None:
        self._find_valid_code(email, code)

        user = self.user_repository.find_by_email(email)
        if not user:
            raise UserNotFoundException()

        user.password = bcrypt.hashpw(
            new_password.encode("utf-8"), bcrypt.gensalt()
        ).decode("utf-8")

        self.repository.delete_by_email(email)

    def _find_valid_code(self, email: str, code: str) -> UserPasswordResetCode:
        entity = self.repository.find_latest_by_email(email)
        if not entity:
            raise UserPasswordResetInvalidException()
        if entity.expired_at < datetime.now():
            raise UserPasswordResetExpiredException()
        if entity.code != code:
            raise UserPasswordResetInvalidException()
        return entity
