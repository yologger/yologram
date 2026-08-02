from datetime import timedelta

import pytest
import redis
from testcontainers.redis import RedisContainer

from app.infra.cache.cache import Cache
from app.infra.cache.redis_cache_service import RedisCacheService


@pytest.fixture(scope="module")
def redis_client():
    # api-v1과 동일하게 Valkey 기준 검증 (ElastiCache Valkey 미러)
    with RedisContainer("valkey/valkey:8-alpine") as container:
        client = redis.Redis(
            host=container.get_container_host_ip(),
            port=int(container.get_exposed_port(6379)),
            socket_connect_timeout=1,
            socket_timeout=1,
            decode_responses=True,
        )
        yield client
        client.close()


@pytest.fixture(autouse=True)
def clean(redis_client):
    redis_client.flushall()


@pytest.fixture
def service(redis_client):
    return RedisCacheService(client=redis_client)


def _broken_service() -> RedisCacheService:
    """접속 불가 클라이언트 — 장애 상황 재현 (즉시 연결 거부)."""
    broken = redis.Redis(
        host="127.0.0.1",
        port=6399,  # 미사용 포트
        socket_connect_timeout=0.2,
        socket_timeout=0.2,
        decode_responses=True,
    )
    return RedisCacheService(client=broken)


class TestRedisCacheService:

    def test_set_후_get_or_null로_값을_읽는다(self, service):
        cache = Cache.user_nickname(1)

        service.set(cache, "요로그램")

        assert service.get_or_null(cache) == "요로그램"

    def test_값은_JSON으로_저장된다__api_v1_Jackson_호환(self, service, redis_client):
        cache = Cache.user_nickname(1)

        service.set(cache, "요로그램")

        assert redis_client.get(cache.key) == '"요로그램"'  # JSON 문자열 표현

    def test_set은_TTL을_설정한다(self, service, redis_client):
        cache = Cache.user_nickname(1)

        service.set(cache, "요로그램")

        ttl = redis_client.ttl(cache.key)
        assert 0 < ttl <= int(timedelta(hours=1).total_seconds())

    def test_없는_키는_None을_반환한다(self, service):
        assert service.get_or_null(Cache.user_nickname(404)) is None

    def test_get_all_as_map은_부분_히트_시_미스_키를_제외한다(self, service):
        hit1, hit2, miss = Cache.user_nickname(1), Cache.user_nickname(2), Cache.user_nickname(3)
        service.set(hit1, "닉1")
        service.set(hit2, "닉2")

        result = service.get_all_as_map([hit1, hit2, miss])

        assert result == {hit1.key: "닉1", hit2.key: "닉2"}

    def test_get_all_as_map_빈_입력은_빈_dict(self, service):
        assert service.get_all_as_map([]) == {}

    def test_set_all은_전부_저장하고_TTL도_설정한다(self, service, redis_client):
        c1, c2 = Cache.user_nickname(1), Cache.user_nickname(2)

        service.set_all({c1: "닉1", c2: "닉2"})

        assert service.get_or_null(c1) == "닉1"
        assert service.get_or_null(c2) == "닉2"
        assert redis_client.ttl(c1.key) > 0

    def test_delete_all_후에는_미스가_된다(self, service):
        c1, c2 = Cache.user_nickname(1), Cache.user_nickname(2)
        service.set_all({c1: "닉1", c2: "닉2"})

        service.delete_all(c1, c2)

        assert service.get_or_null(c1) is None
        assert service.get_or_null(c2) is None

    # --- 장애 폴백 (연결 불가 — 전 연산 삼킴) ---

    def test_장애_시_get_or_null은_None(self):
        assert _broken_service().get_or_null(Cache.user_nickname(1)) is None

    def test_장애_시_get_all_as_map은_빈_dict(self):
        assert _broken_service().get_all_as_map([Cache.user_nickname(1)]) == {}

    def test_장애_시_set_set_all_delete_all은_예외를_던지지_않는다(self):
        service = _broken_service()
        cache = Cache.user_nickname(1)

        service.set(cache, "요로그램")
        service.set_all({cache: "요로그램"})
        service.delete_all(cache)  # 예외 없이 통과하면 성공
