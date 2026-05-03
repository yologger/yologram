from app.config.settings import Settings


class TestService:
    def __init__(self, settings: Settings):
        self.settings = settings

    def get_profile(self) -> str:
        return self.settings.app_profile

    def get_property(self, key: str) -> str | None:
        return self.settings.get_property(key)
