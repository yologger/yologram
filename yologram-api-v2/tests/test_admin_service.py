import os
from unittest.mock import MagicMock

import bcrypt
import pytest

os.environ["JWT_SECRET"] = "test-jwt-secret-key-for-testing"
os.environ["ADMIN_JWT_SECRET"] = "test-admin-jwt-secret-key-for-testing"

from app.config.settings import get_settings
from app.core.exception import (
    AdminUserDuplicateException,
    AdminUserNotFoundException,
    AuthWrongPasswordException,
)
from app.domain.ums.admin_schema import AdminAuthData, AdminLoginRequest, AdminUserCreateRequest
from app.domain.ums.admin_service import AdminUserService
from app.domain.ums.enum import AdminUserRole
from app.domain.ums.model import AdminUser


# lru_cache 초기화
get_settings.cache_clear()


class TestAdminUserService:

    class TestCreate:

        def setup_method(self):
            self.db = MagicMock()
            self.service = AdminUserService(self.db)

        def test_어드민_생성_성공(self):
            self.service.repository.find_by_email = MagicMock(return_value=None)
            saved = AdminUser(email="admin@yologram.link", name="어드민", password="hashed")
            saved.id = 1
            self.service.repository.save = MagicMock(return_value=saved)
            request = AdminUserCreateRequest(email="admin@yologram.link", name="어드민", password="password123!")

            result = self.service.create(request)

            assert result.uid == 1
            created = self.service.repository.save.call_args[0][0]
            assert created.email == "admin@yologram.link"
            assert created.name == "어드민"
            # 비밀번호는 BCrypt 해시로 저장
            assert created.password != "password123!"
            assert bcrypt.checkpw("password123!".encode("utf-8"), created.password.encode("utf-8"))
            # API 생성은 항상 ADMIN — OWNER는 DB 직접 조작 전용
            assert created.role == AdminUserRole.ADMIN

        def test_중복_이메일(self):
            self.service.repository.find_by_email = MagicMock(return_value=MagicMock())
            request = AdminUserCreateRequest(email="dup@yologram.link", name="어드민", password="password123!")

            with pytest.raises(AdminUserDuplicateException):
                self.service.create(request)

    class TestLogin:

        def setup_method(self):
            self.db = MagicMock()
            self.service = AdminUserService(self.db)
            self.hashed_pw = bcrypt.hashpw("password123!".encode("utf-8"), bcrypt.gensalt()).decode("utf-8")
            self.admin = AdminUser(email="admin@yologram.link", name="어드민", password=self.hashed_pw)
            self.admin.id = 1
            self.admin.role = AdminUserRole.ADMIN

        def test_로그인_성공(self):
            self.service.repository.find_by_email = MagicMock(return_value=self.admin)
            request = AdminLoginRequest(email="admin@yologram.link", password="password123!")

            result = self.service.login(request)

            assert result.uid == 1
            assert result.email == "admin@yologram.link"
            assert result.name == "어드민"
            assert result.role == AdminUserRole.ADMIN  # 응답에 role 포함
            assert result.access_token is not None

        def test_존재하지_않는_어드민(self):
            self.service.repository.find_by_email = MagicMock(return_value=None)
            request = AdminLoginRequest(email="notfound@yologram.link", password="password123!")

            with pytest.raises(AdminUserNotFoundException):
                self.service.login(request)

        def test_비밀번호_불일치(self):
            self.service.repository.find_by_email = MagicMock(return_value=self.admin)
            request = AdminLoginRequest(email="admin@yologram.link", password="wrongpassword")

            with pytest.raises(AuthWrongPasswordException):
                self.service.login(request)

    class TestValidateToken:

        def setup_method(self):
            self.db = MagicMock()
            self.service = AdminUserService(self.db)
            self.admin = AdminUser(email="admin@yologram.link", name="어드민", password="hashed")
            self.admin.id = 1
            self.admin.role = AdminUserRole.OWNER

        def test_토큰_검증_성공(self):
            self.service.repository.find_by_id = MagicMock(return_value=self.admin)
            auth_data = AdminAuthData(uid=1, access_token="valid-token")

            result = self.service.validate_token(auth_data)

            assert result.uid == 1
            assert result.email == "admin@yologram.link"
            assert result.name == "어드민"
            assert result.role == AdminUserRole.OWNER  # 응답에 role 포함

        def test_어드민_없음(self):
            self.service.repository.find_by_id = MagicMock(return_value=None)
            auth_data = AdminAuthData(uid=999, access_token="any-token")

            with pytest.raises(AdminUserNotFoundException):
                self.service.validate_token(auth_data)

    class TestLogout:

        def setup_method(self):
            self.db = MagicMock()
            self.service = AdminUserService(self.db)

        def test_로그아웃_성공(self):
            auth_data = AdminAuthData(uid=1, access_token="valid-token")
            self.service.logout(auth_data)
