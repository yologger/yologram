from collections.abc import Generator

from sqlalchemy import create_engine
from sqlalchemy.orm import DeclarativeBase, Session, sessionmaker

from app.config.settings import get_settings


class Base(DeclarativeBase):
    pass


settings = get_settings()

# 커넥션 타임아웃 방어 — 죽은 커넥션(half-open)은 초 단위로 실패해야 한다.
# PyMySQL 기본 read_timeout은 무한이라, Mac 슬립 등으로 커넥션이 죽으면 pool_pre_ping의
# SELECT 1이 영원히 블로킹돼 서비스 전면 hang이 됨(2026-08-12 로컬 재현, done.md).
CONNECT_ARGS = {"connect_timeout": 5, "read_timeout": 10, "write_timeout": 10}

# pool_recycle 15분은 낡은 커넥션 주기 교체 — 번장 bun-ums-api hikari max-lifetime 미러
POOL_RECYCLE_SECONDS = 900

engine = create_engine(
    settings.get_database_url(),
    pool_pre_ping=True,
    pool_recycle=POOL_RECYCLE_SECONDS,
    connect_args=CONNECT_ARGS,
)
SessionLocal = sessionmaker(bind=engine)


def get_db() -> Generator[Session, None, None]:
    db = SessionLocal()
    try:
        yield db
        db.commit()
    except Exception:
        db.rollback()
        raise
    finally:
        db.close()
