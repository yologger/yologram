from app.config.settings import Settings, get_settings
from app.domain.test.service import TestService


def get_test_service() -> TestService:
    return TestService(settings=get_settings())
