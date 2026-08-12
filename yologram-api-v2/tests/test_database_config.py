from app.config.database import CONNECT_ARGS, POOL_RECYCLE_SECONDS, engine


class TestDatabaseEngineConfig:
    """커넥션 타임아웃 방어 설정 계약 — 죽은 커넥션이 무한 대기하지 않게 하는 값들 (done.md)."""

    def test_pool_recycle은_15분이다(self):
        # 번장 bun-ums-api hikari max-lifetime(900000ms) 미러 — 낡은 커넥션 주기 교체
        assert POOL_RECYCLE_SECONDS == 900
        assert engine.pool._recycle == POOL_RECYCLE_SECONDS

    def test_커넥션_타임아웃이_설정되어_있다(self):
        # PyMySQL 기본 read_timeout 무한 → half-open 커넥션에서 pre_ping 무한 블로킹 방지
        assert CONNECT_ARGS == {"connect_timeout": 5, "read_timeout": 10, "write_timeout": 10}

    def test_pre_ping이_켜져_있다(self):
        assert engine.pool._pre_ping is True
