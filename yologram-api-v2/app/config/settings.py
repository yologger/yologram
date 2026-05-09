from functools import lru_cache

from pydantic_settings import BaseSettings, SettingsConfigDict


class Settings(BaseSettings):

    # pydantic-settings: 필드명을 대문자 환경변수로 자동 매핑

    # 우선순위 1. .env 파일에서 주입
    app_name: str = "yologram-api-v2"
    app_profile: str = "default"

    # OTEL_EXPORTER_OTLP_ENDPOINT, OTEL_EXPORTER_OTLP_HEADERS는
    # OpenTelemetry SDK가 자동으로 읽음 (ECS secrets에서 주입)

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
