from functools import lru_cache

from pydantic_settings import BaseSettings, SettingsConfigDict


class Settings(BaseSettings):
    app_name: str = "yologram-api-v2"
    app_profile: str = "default"

    model_config = SettingsConfigDict(
        env_file=".env",
        env_file_encoding="utf-8",
        extra="ignore",
    )

    def get_property(self, key: str) -> str | None:
        return getattr(self, key, None)


@lru_cache
def get_settings() -> Settings:
    return Settings()
