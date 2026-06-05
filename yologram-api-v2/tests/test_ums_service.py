from unittest.mock import MagicMock

import pytest

from app.core.exception import UserDuplicateException
from app.domain.ums.model import User
from app.domain.ums.schema import JoinRequest
from app.domain.ums.service import UserService


class TestUserService:

    def setup_method(self):
        self.db = MagicMock()
        self.service = UserService(self.db)

    class TestJoin:

        def setup_method(self):
            self.db = MagicMock()
            self.service = UserService(self.db)
            self.request = JoinRequest(
                email="test@yologram.link",
                name="테스트",
                nickname="tester",
                password="password123!",
            )

        def test_회원가입_성공(self):
            self.service.repository.find_by_email = MagicMock(return_value=None)
            saved_user = User(email=self.request.email, name=self.request.name, nickname=self.request.nickname, password="hashed")
            saved_user.id = 1
            self.service.repository.save = MagicMock(return_value=saved_user)

            result = self.service.join(self.request)

            assert result.uid == 1
            self.service.repository.find_by_email.assert_called_once_with("test@yologram.link")
            self.service.repository.save.assert_called_once()

        def test_이메일_중복_시_예외(self):
            existing_user = User(email=self.request.email, name="기존", nickname="existing", password="hashed")
            self.service.repository.find_by_email = MagicMock(return_value=existing_user)

            with pytest.raises(UserDuplicateException):
                self.service.join(self.request)
