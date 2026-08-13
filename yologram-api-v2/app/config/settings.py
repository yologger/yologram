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

    # 어드민 JWT 설정 (유저 JWT와 secret·audience 분리, ADMIN_JWT_SECRET은 ECS secrets 또는 환경변수로 주입)
    admin_jwt_secret: str = ""
    admin_jwt_expire: int = 86400
    admin_jwt_issuer: str = "yologram.link"
    admin_jwt_audience: str = "yologram.admin"

    # SES
    ses_from_address: str = "no-reply@yologram.link"

    # 캐시용 Redis(Valkey) — SSM 키는 api-v1과 공유 개념(cache.data.redis.host)이며
    # pydantic-settings 대문자 env 매핑으로 CACHE_REDIS_HOST/CACHE_REDIS_PORT로 주입. 로컬 기본 localhost
    cache_redis_host: str = "localhost"
    cache_redis_port: int = 6379

    # 이벤트 스트림(Kinesis) — 게시글 조회 이벤트 발행 대상 스트림 이름 (env POST_VIEW_STREAM_NAME).
    # 스트림 이름은 비밀값이 아니라 고정 이름이므로 Parameter Store가 아닌 설정 기본값/환경변수에 직접 둔다
    # (api-v1 application.yaml event.stream.post-view.name과 동일 판단).
    # 기본값은 빈 값 = 발행 스킵 (로컬·테스트에서 prod 스트림이 오염되지 않도록).
    # prod는 컨테이너 이미지에서 주입 (Dockerfile ENV — api-v1 application-prod.yaml에 대응)
    post_view_stream_name: str = ""

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
