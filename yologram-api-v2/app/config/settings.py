from functools import lru_cache

from pydantic_settings import BaseSettings, SettingsConfigDict


class Settings(BaseSettings):

    # pydantic-settings: 필드명을 대문자 환경변수로 자동 매핑

    app_name: str = "yologram-api-v2"
    app_profile: str = "default"

    # DB 설정 (ECS secrets 또는 환경변수로 주입)
    db_url: str = ""
    db_username: str = ""
    db_password: str = ""

    # JWT 설정 (JWT_SECRET은 ECS secrets 또는 환경변수로 주입)
    jwt_secret: str = ""
    jwt_expire: int = 86400
    jwt_issuer: str = "yologram.link"
    jwt_audience: str = "yologram.client"

    # OTEL_EXPORTER_OTLP_ENDPOINT, OTEL_EXPORTER_OTLP_HEADERS는
    # OpenTelemetry SDK가 자동으로 읽음 (ECS secrets에서 주입)

    model_config = SettingsConfigDict(
        env_file=".env",
        env_file_encoding="utf-8",
        extra="ignore",
    )

    def get_database_url(self) -> str:
        if "://" in self.db_url:
            # mysql+pymysql://host:port/db → mysql+pymysql://user:pass@host:port/db
            scheme, rest = self.db_url.split("://", 1)
            return f"{scheme}://{self.db_username}:{self.db_password}@{rest}"
        return f"mysql+pymysql://{self.db_username}:{self.db_password}@{self.db_url}"

    def get_property(self, key: str) -> str | None:
        return getattr(self, key, None)


@lru_cache
def get_settings() -> Settings:
    return Settings()
