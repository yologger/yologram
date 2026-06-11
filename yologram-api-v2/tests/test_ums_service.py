from unittest.mock import MagicMock

import pytest

from app.core.exception import AuthWrongPasswordException, UserDuplicateException, UserNotFoundException
from app.domain.ums.enum import UserType
from app.domain.ums.model import User
from app.domain.ums.schema import ChangePasswordRequest, JoinRequest, UpdateProfileRequest
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

    class TestGetMe:

        def setup_method(self):
            self.db = MagicMock()
            self.service = UserService(self.db)

        def _make_user(self, uid=1, avatar=None):
            user = MagicMock()
            user.id = uid
            user.email = "test@yologram.link"
            user.name = "테스트"
            user.nickname = "tester"
            user.avatar = avatar
            user.type = UserType.DEFAULT
            user.joined_date = "2025-01-01T00:00:00"
            return user

        def test_회원정보_조회_성공(self):
            user = self._make_user()
            self.service.repository.find_by_id = MagicMock(return_value=user)

            result = self.service.get_me(1)

            assert result.uid == 1
            assert result.email == "test@yologram.link"
            assert result.name == "테스트"
            assert result.nickname == "tester"
            assert result.avatar is None
            assert result.type == "DEFAULT"

        def test_아바타가_있으면_포함(self):
            user = self._make_user(avatar="https://example.com/avatar.png")
            self.service.repository.find_by_id = MagicMock(return_value=user)

            result = self.service.get_me(1)

            assert result.avatar == "https://example.com/avatar.png"

        def test_존재하지_않는_유저_시_예외(self):
            self.service.repository.find_by_id = MagicMock(return_value=None)

            with pytest.raises(UserNotFoundException):
                self.service.get_me(999)

    class TestUpdateProfile:

        def setup_method(self):
            self.db = MagicMock()
            self.service = UserService(self.db)

        def _make_user(self, uid=1):
            user = MagicMock()
            user.id = uid
            user.email = "test@yologram.link"
            user.name = "테스트"
            user.nickname = "tester"
            user.avatar = None
            user.type = UserType.DEFAULT
            user.joined_date = "2025-01-01T00:00:00"
            return user

        def test_닉네임_변경_성공(self):
            user = self._make_user()
            self.service.repository.find_by_id = MagicMock(return_value=user)

            request = UpdateProfileRequest(nickname="new-nickname")
            result = self.service.update_profile(1, request)

            assert user.nickname == "new-nickname"
            assert result.nickname == "new-nickname"

        def test_변경된_유저_정보를_반환한다(self):
            user = self._make_user()
            self.service.repository.find_by_id = MagicMock(return_value=user)

            request = UpdateProfileRequest(nickname="new-nickname")
            result = self.service.update_profile(1, request)

            assert result.uid == 1
            assert result.email == "test@yologram.link"
            assert result.name == "테스트"

        def test_존재하지_않는_유저_시_예외(self):
            self.service.repository.find_by_id = MagicMock(return_value=None)

            request = UpdateProfileRequest(nickname="new-nickname")

            with pytest.raises(UserNotFoundException):
                self.service.update_profile(999, request)

    class TestChangePassword:

        def setup_method(self):
            self.db = MagicMock()
            self.service = UserService(self.db)

        def _make_user(self, password="password123!"):
            import bcrypt
            hashed = bcrypt.hashpw(password.encode("utf-8"), bcrypt.gensalt()).decode("utf-8")
            user = MagicMock()
            user.id = 1
            user.password = hashed
            return user

        def test_비밀번호_변경_성공(self):
            user = self._make_user()
            self.service.repository.find_by_id = MagicMock(return_value=user)

            request = ChangePasswordRequest(currentPassword="password123!", newPassword="newpass1234")
            self.service.change_password(1, request)

            assert user.password != "password123!"

        def test_현재_비밀번호_불일치_시_예외(self):
            user = self._make_user()
            self.service.repository.find_by_id = MagicMock(return_value=user)

            request = ChangePasswordRequest(currentPassword="wrongpass", newPassword="newpass1234")

            with pytest.raises(AuthWrongPasswordException):
                self.service.change_password(1, request)

        def test_존재하지_않는_유저_시_예외(self):
            self.service.repository.find_by_id = MagicMock(return_value=None)

            request = ChangePasswordRequest(currentPassword="password123!", newPassword="newpass1234")

            with pytest.raises(UserNotFoundException):
                self.service.change_password(999, request)
