from datetime import datetime, timedelta
from unittest.mock import MagicMock

import pytest

from app.core.exception import (
    UserPasswordResetExpiredException,
    UserPasswordResetInvalidException,
    UserNotFoundException,
)
from app.domain.ums.user_password_reset_service import UserPasswordResetService


class TestUserPasswordResetService:

    def setup_method(self):
        self.db = MagicMock()
        self.email_sender = MagicMock()
        self.service = UserPasswordResetService(self.db, self.email_sender)

    class TestSendCode:

        def setup_method(self):
            self.db = MagicMock()
            self.email_sender = MagicMock()
            self.service = UserPasswordResetService(self.db, self.email_sender)
            self.service.user_repository.find_by_email = MagicMock(return_value=MagicMock())
            self.service.repository.delete_by_email = MagicMock()
            self.service.repository.save = MagicMock(return_value=MagicMock())

        def test_가입된_이메일이면_코드를_발송한다(self):
            self.service.send_code("test@yologram.link")

            self.service.repository.delete_by_email.assert_called_once_with("test@yologram.link")
            self.service.repository.save.assert_called_once()
            saved = self.service.repository.save.call_args[0][0]
            assert saved.email == "test@yologram.link"
            assert len(saved.code) == 6 and saved.code.isdigit()
            self.email_sender.send_password_reset_code.assert_called_once_with("test@yologram.link", saved.code)

        def test_가입되지_않은_이메일이면_예외(self):
            self.service.user_repository.find_by_email = MagicMock(return_value=None)

            with pytest.raises(UserNotFoundException):
                self.service.send_code("unknown@yologram.link")

            self.email_sender.send_password_reset_code.assert_not_called()

    class TestVerifyCode:

        def setup_method(self):
            self.db = MagicMock()
            self.service = UserPasswordResetService(self.db, MagicMock())

        def test_올바른_코드면_verified(self):
            entity = MagicMock()
            entity.code = "123456"
            entity.expired_at = datetime.now() + timedelta(minutes=3)
            entity.verified = False
            self.service.repository.find_latest_by_email = MagicMock(return_value=entity)

            self.service.verify_code("test@yologram.link", "123456")

            assert entity.verified is True

        def test_레코드_없으면_예외(self):
            self.service.repository.find_latest_by_email = MagicMock(return_value=None)

            with pytest.raises(UserPasswordResetInvalidException):
                self.service.verify_code("test@yologram.link", "123456")

        def test_만료면_예외(self):
            entity = MagicMock()
            entity.code = "123456"
            entity.expired_at = datetime.now() - timedelta(minutes=1)
            self.service.repository.find_latest_by_email = MagicMock(return_value=entity)

            with pytest.raises(UserPasswordResetExpiredException):
                self.service.verify_code("test@yologram.link", "123456")

        def test_코드_불일치면_예외(self):
            entity = MagicMock()
            entity.code = "123456"
            entity.expired_at = datetime.now() + timedelta(minutes=3)
            self.service.repository.find_latest_by_email = MagicMock(return_value=entity)

            with pytest.raises(UserPasswordResetInvalidException):
                self.service.verify_code("test@yologram.link", "999999")

    class TestConfirm:

        def setup_method(self):
            self.db = MagicMock()
            self.service = UserPasswordResetService(self.db, MagicMock())
            self.service.repository.delete_by_email = MagicMock()

        def _valid_code(self):
            entity = MagicMock()
            entity.code = "123456"
            entity.expired_at = datetime.now() + timedelta(minutes=3)
            return entity

        def test_재검증_후_비밀번호_변경하고_코드_삭제(self):
            self.service.repository.find_latest_by_email = MagicMock(return_value=self._valid_code())
            user = MagicMock()
            user.password = "old"
            self.service.user_repository.find_by_email = MagicMock(return_value=user)

            self.service.confirm("test@yologram.link", "123456", "newpass1234")

            assert user.password != "old"
            self.service.repository.delete_by_email.assert_called_once_with("test@yologram.link")

        def test_코드_불일치면_예외(self):
            self.service.repository.find_latest_by_email = MagicMock(return_value=self._valid_code())

            with pytest.raises(UserPasswordResetInvalidException):
                self.service.confirm("test@yologram.link", "999999", "newpass1234")

        def test_만료면_예외(self):
            entity = MagicMock()
            entity.code = "123456"
            entity.expired_at = datetime.now() - timedelta(minutes=1)
            self.service.repository.find_latest_by_email = MagicMock(return_value=entity)

            with pytest.raises(UserPasswordResetExpiredException):
                self.service.confirm("test@yologram.link", "123456", "newpass1234")
