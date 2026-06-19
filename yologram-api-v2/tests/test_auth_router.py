import os
from unittest.mock import MagicMock, patch

import bcrypt
import pytest
from fastapi.testclient import TestClient

os.environ["JWT_SECRET"] = "test-jwt-secret-key-for-testing"

from app.config.database import get_db
from app.domain.ums.email_dependency import get_email_sender
from app.domain.ums.jwt_util import create_token
from app.domain.ums.model import User
from app.main import app


class TestAuthRouter:

    class TestLogin:

        def setup_method(self):
            self.mock_db = MagicMock()
            app.dependency_overrides[get_db] = lambda: self.mock_db
            self.client = TestClient(app)
            self.hashed_pw = bcrypt.hashpw("password123!".encode("utf-8"), bcrypt.gensalt()).decode("utf-8")

        def teardown_method(self):
            app.dependency_overrides.clear()

        @patch("app.domain.ums.auth_service.UserRepository")
        def test_로그인_성공(self, mock_repo_cls):
            user = User(email="test@yologram.link", name="테스터", nickname="tester", password=self.hashed_pw)
            user.id = 1
            mock_repo = MagicMock()
            mock_repo.find_by_email.return_value = user
            mock_repo_cls.return_value = mock_repo

            response = self.client.post("/api/v2/ums/auth/login", json={
                "email": "test@yologram.link",
                "password": "password123!",
            })

            assert response.status_code == 200
            data = response.json()["data"]
            assert data["uid"] == 1
            assert data["email"] == "test@yologram.link"
            assert data["name"] == "테스터"
            assert data["nickname"] == "tester"
            assert "accessToken" in data

        @patch("app.domain.ums.auth_service.UserRepository")
        def test_존재하지_않는_사용자_404(self, mock_repo_cls):
            mock_repo = MagicMock()
            mock_repo.find_by_email.return_value = None
            mock_repo_cls.return_value = mock_repo

            response = self.client.post("/api/v2/ums/auth/login", json={
                "email": "notfound@yologram.link",
                "password": "password123!",
            })

            assert response.status_code == 404
            assert response.json()["errorCode"] == "USER_NOT_FOUND"

        @patch("app.domain.ums.auth_service.UserRepository")
        def test_비밀번호_불일치_401(self, mock_repo_cls):
            user = User(email="test@yologram.link", name="테스터", nickname="tester", password=self.hashed_pw)
            user.id = 1
            mock_repo = MagicMock()
            mock_repo.find_by_email.return_value = user
            mock_repo_cls.return_value = mock_repo

            response = self.client.post("/api/v2/ums/auth/login", json={
                "email": "test@yologram.link",
                "password": "wrongpassword",
            })

            assert response.status_code == 401
            assert response.json()["errorCode"] == "AUTH_WRONG_PASSWORD"

        def test_이메일_형식_오류_400(self):
            response = self.client.post("/api/v2/ums/auth/login", json={
                "email": "invalid",
                "password": "password123!",
            })

            assert response.status_code == 400

    class TestValidateToken:

        def setup_method(self):
            self.mock_db = MagicMock()
            app.dependency_overrides[get_db] = lambda: self.mock_db
            self.client = TestClient(app)

        def teardown_method(self):
            app.dependency_overrides.clear()

        @patch("app.domain.ums.auth_service.UserRepository")
        def test_토큰_검증_성공(self, mock_repo_cls):
            token = create_token(1)
            user = User(email="test@yologram.link", name="테스터", nickname="tester", password="hashed")
            user.id = 1
            mock_repo = MagicMock()
            mock_repo.find_by_id.return_value = user
            mock_repo_cls.return_value = mock_repo

            response = self.client.post(
                "/api/v2/ums/auth/validate-token",
                headers={"Authorization": f"Bearer {token}"},
            )

            assert response.status_code == 200
            data = response.json()["data"]
            assert data["uid"] == 1
            assert data["email"] == "test@yologram.link"

        def test_헤더_없음_401(self):
            response = self.client.post("/api/v2/ums/auth/validate-token")

            assert response.status_code == 401
            assert response.json()["errorCode"] == "AUTH_TOKEN_INVALID"

        def test_빈_Bearer_401(self):
            response = self.client.post(
                "/api/v2/ums/auth/validate-token",
                headers={"Authorization": "Bearer "},
            )

            assert response.status_code == 401

        def test_Basic_스킴_401(self):
            response = self.client.post(
                "/api/v2/ums/auth/validate-token",
                headers={"Authorization": "Basic abc123"},
            )

            assert response.status_code == 401

        def test_만료된_토큰_401(self):
            import time
            import jwt as pyjwt
            from app.config.settings import get_settings

            settings = get_settings()
            payload = {
                "uid": 1,
                "iss": settings.jwt_issuer,
                "aud": settings.jwt_audience,
                "iat": time.time() - 100,
                "exp": time.time() - 10,
            }
            token = pyjwt.encode(payload, settings.jwt_secret, algorithm="HS256")

            response = self.client.post(
                "/api/v2/ums/auth/validate-token",
                headers={"Authorization": f"Bearer {token}"},
            )

            assert response.status_code == 401
            assert response.json()["errorCode"] == "AUTH_TOKEN_EXPIRED"

    class TestLogout:

        def setup_method(self):
            self.mock_db = MagicMock()
            app.dependency_overrides[get_db] = lambda: self.mock_db
            self.client = TestClient(app)

        def teardown_method(self):
            app.dependency_overrides.clear()

        @patch("app.domain.ums.auth_service.UserRepository")
        def test_로그아웃_성공_204(self, mock_repo_cls):
            token = create_token(1)
            mock_repo = MagicMock()
            mock_repo_cls.return_value = mock_repo

            response = self.client.post(
                "/api/v2/ums/auth/logout",
                headers={"Authorization": f"Bearer {token}"},
            )

            assert response.status_code == 204

        def test_헤더_없음_401(self):
            response = self.client.post("/api/v2/ums/auth/logout")

            assert response.status_code == 401

    class TestSendVerificationCode:

        def setup_method(self):
            self.mock_db = MagicMock()
            self.mock_email_sender = MagicMock()
            app.dependency_overrides[get_db] = lambda: self.mock_db
            app.dependency_overrides[get_email_sender] = lambda: self.mock_email_sender
            self.client = TestClient(app)

        def teardown_method(self):
            app.dependency_overrides.clear()

        @patch("app.domain.ums.email_verification_service.UserRepository")
        @patch("app.domain.ums.email_verification_service.EmailVerificationCodeRepository")
        def test_인증_코드_발송_성공_204(self, mock_repo_cls, mock_user_repo_cls):
            mock_repo = MagicMock()
            mock_repo.save.return_value = MagicMock()
            mock_repo_cls.return_value = mock_repo

            mock_user_repo = MagicMock()
            mock_user_repo.find_by_email.return_value = None
            mock_user_repo_cls.return_value = mock_user_repo

            response = self.client.post("/api/v2/ums/auth/email-verification/send", json={
                "email": "test@yologram.link",
            })

            assert response.status_code == 204
            self.mock_email_sender.send_verification_code.assert_called_once()

        @patch("app.domain.ums.email_verification_service.UserRepository")
        @patch("app.domain.ums.email_verification_service.EmailVerificationCodeRepository")
        def test_이미_가입된_이메일_409(self, mock_repo_cls, mock_user_repo_cls):
            mock_repo = MagicMock()
            mock_repo_cls.return_value = mock_repo

            mock_user_repo = MagicMock()
            mock_user_repo.find_by_email.return_value = MagicMock()
            mock_user_repo_cls.return_value = mock_user_repo

            response = self.client.post("/api/v2/ums/auth/email-verification/send", json={
                "email": "duplicate@yologram.link",
            })

            assert response.status_code == 409
            assert response.json()["errorCode"] == "USER_DUPLICATE"

        def test_이메일_형식_오류_400(self):
            response = self.client.post("/api/v2/ums/auth/email-verification/send", json={
                "email": "invalid",
            })

            assert response.status_code == 400

    class TestVerifyEmail:

        def setup_method(self):
            self.mock_db = MagicMock()
            app.dependency_overrides[get_db] = lambda: self.mock_db
            self.client = TestClient(app)

        def teardown_method(self):
            app.dependency_overrides.clear()

        @patch("app.domain.ums.email_verification_service.EmailVerificationCodeRepository")
        def test_인증_코드_확인_성공_204(self, mock_repo_cls):
            from datetime import datetime, timedelta
            entity = MagicMock()
            entity.code = "123456"
            entity.expired_at = datetime.now() + timedelta(minutes=3)
            entity.verified = False
            mock_repo = MagicMock()
            mock_repo.find_latest_by_email.return_value = entity
            mock_repo_cls.return_value = mock_repo

            response = self.client.post("/api/v2/ums/auth/email-verification/verify", json={
                "email": "test@yologram.link",
                "code": "123456",
            })

            assert response.status_code == 204

        @patch("app.domain.ums.email_verification_service.EmailVerificationCodeRepository")
        def test_레코드_없음_400(self, mock_repo_cls):
            mock_repo = MagicMock()
            mock_repo.find_latest_by_email.return_value = None
            mock_repo_cls.return_value = mock_repo

            response = self.client.post("/api/v2/ums/auth/email-verification/verify", json={
                "email": "test@yologram.link",
                "code": "000000",
            })

            assert response.status_code == 400
            assert response.json()["errorCode"] == "EMAIL_VERIFICATION_INVALID"

        @patch("app.domain.ums.email_verification_service.EmailVerificationCodeRepository")
        def test_코드_불일치_400(self, mock_repo_cls):
            from datetime import datetime, timedelta
            entity = MagicMock()
            entity.code = "123456"
            entity.expired_at = datetime.now() + timedelta(minutes=3)
            mock_repo = MagicMock()
            mock_repo.find_latest_by_email.return_value = entity
            mock_repo_cls.return_value = mock_repo

            response = self.client.post("/api/v2/ums/auth/email-verification/verify", json={
                "email": "test@yologram.link",
                "code": "000000",
            })

            assert response.status_code == 400
            assert response.json()["errorCode"] == "EMAIL_VERIFICATION_INVALID"

        @patch("app.domain.ums.email_verification_service.EmailVerificationCodeRepository")
        def test_만료된_코드_400(self, mock_repo_cls):
            from datetime import datetime, timedelta
            entity = MagicMock()
            entity.code = "123456"
            entity.expired_at = datetime.now() - timedelta(minutes=1)
            mock_repo = MagicMock()
            mock_repo.find_latest_by_email.return_value = entity
            mock_repo_cls.return_value = mock_repo

            response = self.client.post("/api/v2/ums/auth/email-verification/verify", json={
                "email": "test@yologram.link",
                "code": "123456",
            })

            assert response.status_code == 400
            assert response.json()["errorCode"] == "EMAIL_VERIFICATION_EXPIRED"

        def test_코드_길이_5자리_400(self):
            response = self.client.post("/api/v2/ums/auth/email-verification/verify", json={
                "email": "test@yologram.link",
                "code": "12345",
            })

            assert response.status_code == 400

        def test_코드_길이_7자리_400(self):
            response = self.client.post("/api/v2/ums/auth/email-verification/verify", json={
                "email": "test@yologram.link",
                "code": "1234567",
            })

            assert response.status_code == 400

    class TestPasswordResetSend:

        def setup_method(self):
            self.mock_db = MagicMock()
            self.mock_email_sender = MagicMock()
            app.dependency_overrides[get_db] = lambda: self.mock_db
            app.dependency_overrides[get_email_sender] = lambda: self.mock_email_sender
            self.client = TestClient(app)

        def teardown_method(self):
            app.dependency_overrides.clear()

        @patch("app.domain.ums.password_reset_service.UserRepository")
        @patch("app.domain.ums.password_reset_service.PasswordResetCodeRepository")
        def test_발송_성공_204(self, mock_repo_cls, mock_user_repo_cls):
            mock_repo = MagicMock()
            mock_repo.save.return_value = MagicMock()
            mock_repo_cls.return_value = mock_repo

            mock_user_repo = MagicMock()
            mock_user_repo.find_by_email.return_value = MagicMock()
            mock_user_repo_cls.return_value = mock_user_repo

            response = self.client.post("/api/v2/ums/auth/password-reset/send", json={
                "email": "test@yologram.link",
            })

            assert response.status_code == 204
            self.mock_email_sender.send_password_reset_code.assert_called_once()

        @patch("app.domain.ums.password_reset_service.UserRepository")
        @patch("app.domain.ums.password_reset_service.PasswordResetCodeRepository")
        def test_미가입_이메일_404(self, mock_repo_cls, mock_user_repo_cls):
            mock_repo_cls.return_value = MagicMock()

            mock_user_repo = MagicMock()
            mock_user_repo.find_by_email.return_value = None
            mock_user_repo_cls.return_value = mock_user_repo

            response = self.client.post("/api/v2/ums/auth/password-reset/send", json={
                "email": "unknown@yologram.link",
            })

            assert response.status_code == 404
            assert response.json()["errorCode"] == "USER_NOT_FOUND"

        def test_이메일_형식_오류_400(self):
            response = self.client.post("/api/v2/ums/auth/password-reset/send", json={
                "email": "invalid",
            })

            assert response.status_code == 400

    class TestPasswordResetVerify:

        def setup_method(self):
            self.mock_db = MagicMock()
            app.dependency_overrides[get_db] = lambda: self.mock_db
            self.client = TestClient(app)

        def teardown_method(self):
            app.dependency_overrides.clear()

        @patch("app.domain.ums.password_reset_service.PasswordResetCodeRepository")
        def test_검증_성공_204(self, mock_repo_cls):
            from datetime import datetime, timedelta
            entity = MagicMock()
            entity.code = "123456"
            entity.expired_at = datetime.now() + timedelta(minutes=3)
            entity.verified = False
            mock_repo = MagicMock()
            mock_repo.find_latest_by_email.return_value = entity
            mock_repo_cls.return_value = mock_repo

            response = self.client.post("/api/v2/ums/auth/password-reset/verify", json={
                "email": "test@yologram.link",
                "code": "123456",
            })

            assert response.status_code == 204

        @patch("app.domain.ums.password_reset_service.PasswordResetCodeRepository")
        def test_코드_불일치_400(self, mock_repo_cls):
            mock_repo = MagicMock()
            mock_repo.find_latest_by_email.return_value = None
            mock_repo_cls.return_value = mock_repo

            response = self.client.post("/api/v2/ums/auth/password-reset/verify", json={
                "email": "test@yologram.link",
                "code": "000000",
            })

            assert response.status_code == 400
            assert response.json()["errorCode"] == "PASSWORD_RESET_INVALID"

    class TestPasswordResetConfirm:

        def setup_method(self):
            self.mock_db = MagicMock()
            app.dependency_overrides[get_db] = lambda: self.mock_db
            self.client = TestClient(app)

        def teardown_method(self):
            app.dependency_overrides.clear()

        @patch("app.domain.ums.password_reset_service.UserRepository")
        @patch("app.domain.ums.password_reset_service.PasswordResetCodeRepository")
        def test_변경_성공_204(self, mock_repo_cls, mock_user_repo_cls):
            from datetime import datetime, timedelta
            entity = MagicMock()
            entity.code = "123456"
            entity.expired_at = datetime.now() + timedelta(minutes=3)
            mock_repo = MagicMock()
            mock_repo.find_latest_by_email.return_value = entity
            mock_repo_cls.return_value = mock_repo

            mock_user_repo = MagicMock()
            mock_user_repo.find_by_email.return_value = MagicMock()
            mock_user_repo_cls.return_value = mock_user_repo

            response = self.client.post("/api/v2/ums/auth/password-reset/confirm", json={
                "email": "test@yologram.link",
                "code": "123456",
                "newPassword": "newpass1234",
            })

            assert response.status_code == 204

        @patch("app.domain.ums.password_reset_service.PasswordResetCodeRepository")
        def test_코드_만료_400(self, mock_repo_cls):
            from datetime import datetime, timedelta
            entity = MagicMock()
            entity.code = "123456"
            entity.expired_at = datetime.now() - timedelta(minutes=1)
            mock_repo = MagicMock()
            mock_repo.find_latest_by_email.return_value = entity
            mock_repo_cls.return_value = mock_repo

            response = self.client.post("/api/v2/ums/auth/password-reset/confirm", json={
                "email": "test@yologram.link",
                "code": "123456",
                "newPassword": "newpass1234",
            })

            assert response.status_code == 400
            assert response.json()["errorCode"] == "PASSWORD_RESET_EXPIRED"

        def test_새_비밀번호_길이_400(self):
            response = self.client.post("/api/v2/ums/auth/password-reset/confirm", json={
                "email": "test@yologram.link",
                "code": "123456",
                "newPassword": "short",
            })

            assert response.status_code == 400
