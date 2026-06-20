from datetime import datetime, timedelta
from unittest.mock import MagicMock

import pytest

from app.core.exception import (
    UserEmailVerificationExpiredException,
    UserEmailVerificationInvalidException,
    UserDuplicateException,
)
from app.domain.ums.user_email_verification_service import UserEmailVerificationService


class TestUserEmailVerificationService:

    def setup_method(self):
        self.db = MagicMock()
        self.email_sender = MagicMock()
        self.service = UserEmailVerificationService(self.db, self.email_sender)

    class TestSendVerificationCode:

        def setup_method(self):
            self.db = MagicMock()
            self.email_sender = MagicMock()
            self.service = UserEmailVerificationService(self.db, self.email_sender)
            self.service.user_repository.find_by_email = MagicMock(return_value=None)
            self.service.repository.delete_by_email = MagicMock()
            self.service.repository.save = MagicMock(return_value=MagicMock())

        def test_인증_코드_발송_성공(self):
            self.service.send_code("test@yologram.link")

            self.service.repository.save.assert_called_once()
            saved_entity = self.service.repository.save.call_args[0][0]
            assert saved_entity.email == "test@yologram.link"
            assert len(saved_entity.code) == 6
            assert saved_entity.code.isdigit()
            self.email_sender.send_verification_code.assert_called_once_with("test@yologram.link", saved_entity.code)

        def test_인증_코드는_6자리_숫자(self):
            self.service.send_code("test@yologram.link")

            saved_entity = self.service.repository.save.call_args[0][0]
            assert len(saved_entity.code) == 6
            assert saved_entity.code.isdigit()

        def test_만료시간은_5분_후(self):
            before = datetime.now()

            self.service.send_code("test@yologram.link")

            saved_entity = self.service.repository.save.call_args[0][0]
            expected_min = before + timedelta(minutes=5)
            expected_max = datetime.now() + timedelta(minutes=5)
            assert expected_min <= saved_entity.expired_at <= expected_max

        def test_기존_인증_레코드를_삭제하고_새로_생성(self):
            self.service.send_code("test@yologram.link")

            self.service.repository.delete_by_email.assert_called_once_with("test@yologram.link")
            self.service.repository.save.assert_called_once()

        def test_이미_가입된_이메일이면_예외(self):
            self.service.user_repository.find_by_email = MagicMock(return_value=MagicMock())

            with pytest.raises(UserDuplicateException):
                self.service.send_code("duplicate@yologram.link")

            self.email_sender.send_verification_code.assert_not_called()

    class TestVerifyEmail:

        def setup_method(self):
            self.db = MagicMock()
            self.email_sender = MagicMock()
            self.service = UserEmailVerificationService(self.db, self.email_sender)

        def test_인증_성공(self):
            entity = MagicMock()
            entity.code = "123456"
            entity.expired_at = datetime.now() + timedelta(minutes=3)
            entity.verified = False
            self.service.repository.find_latest_by_email = MagicMock(return_value=entity)

            self.service.verify_code("test@yologram.link", "123456")

            assert entity.verified is True

        def test_인증_레코드가_없으면_예외(self):
            self.service.repository.find_latest_by_email = MagicMock(return_value=None)

            with pytest.raises(UserEmailVerificationInvalidException):
                self.service.verify_code("test@yologram.link", "123456")

        def test_코드_불일치_시_예외(self):
            entity = MagicMock()
            entity.code = "123456"
            entity.expired_at = datetime.now() + timedelta(minutes=3)
            self.service.repository.find_latest_by_email = MagicMock(return_value=entity)

            with pytest.raises(UserEmailVerificationInvalidException):
                self.service.verify_code("test@yologram.link", "000000")

        def test_만료된_코드_시_예외(self):
            entity = MagicMock()
            entity.code = "123456"
            entity.expired_at = datetime.now() - timedelta(minutes=1)
            self.service.repository.find_latest_by_email = MagicMock(return_value=entity)

            with pytest.raises(UserEmailVerificationExpiredException):
                self.service.verify_code("test@yologram.link", "123456")
