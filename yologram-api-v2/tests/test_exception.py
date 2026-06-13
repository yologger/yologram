import os

from fastapi.testclient import TestClient

os.environ.setdefault("JWT_SECRET", "test-jwt-secret-key-for-testing")

from app.main import app


class TestExceptionHandler:

    def setup_method(self):
        self.client = TestClient(app)

    def test_존재하지_않는_경로는_404와_NOT_FOUND(self):
        response = self.client.get("/api/v2/ums/auth/not-exists")

        assert response.status_code == 404
        body = response.json()
        assert body["errorCode"] == "NOT_FOUND"
        assert "errorMessage" in body

    def test_허용되지_않은_메서드는_405와_METHOD_NOT_ALLOWED(self):
        # /api/v2/ums/auth/login 은 POST만 허용
        response = self.client.get("/api/v2/ums/auth/login")

        assert response.status_code == 405
        body = response.json()
        assert body["errorCode"] == "METHOD_NOT_ALLOWED"
        assert "errorMessage" in body
