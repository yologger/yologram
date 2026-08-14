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

    # 이벤트 발행(Kinesis) — 게시글 조회 이벤트. api-v1의
    # yologram.events.publish.post-view.{enabled,stream}에 대응하는 두 축을,
    # pydantic-settings 평면 대문자 매핑 관례에 맞춰 POST_VIEW_PUBLISH_ENABLED/POST_VIEW_PUBLISH_STREAM으로 둔다.
    # 스트림 이름은 비밀값이 아니라 고정 이름이므로 Parameter Store가 아닌 설정 기본값/환경변수에 직접 둔다.
    # 기본은 비활성 = 발행 스킵 (로컬·테스트에서 prod 스트림이 오염되지 않도록).
    # prod는 컨테이너 이미지에서 주입 (Dockerfile ENV — api-v1 application-prod.yaml에 대응)
    post_view_publish_enabled: bool = False
    post_view_publish_stream: str = ""

    # 검색 인덱싱 작업 발행(SQS) — 위 Kinesis 발행과 같은 규칙(기본 비활성, prod만 Dockerfile ENV로 주입).
    # api-v1의 yologram.messages.publish.post-index.{enabled,queue}에 대응
    post_index_publish_enabled: bool = False
    post_index_publish_queue: str = ""

    # 검색 (셀프호스팅 OpenSearch) — api-v1 opensearch.main.*에 대응.
    # 스위치는 환경변수, 접속 3개는 prod에서 SSM → 컨테이너 주입.
    # enabled=false(로컬·테스트 기본)면 검색 라우터·서비스를 등록하지 않는다
    opensearch_main_enabled: bool = False
    opensearch_main_uri: str = ""
    opensearch_main_username: str = ""
    opensearch_main_password: str = ""

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
