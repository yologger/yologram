from functools import lru_cache

import redis

from app.config.settings import get_settings


@lru_cache
def get_redis_client() -> redis.Redis:
    """
    캐시용 Redis(Valkey) 클라이언트 — 연결은 lazy (첫 명령 시점). Redis 미기동 환경(테스트 등)에서도 임포트·부팅에 영향 없음.
    캐시는 부가 기능 — Redis 장애가 API 지연으로 전파되지 않도록 connect/command 타임아웃 1초.
    (api-v1 RedisConfig과 동일 근거: 기본값이 길면 요청당 캐시 연산 수만큼 지연이 누적 — 실측 120초 사례)
    """
    settings = get_settings()
    return redis.Redis(
        host=settings.cache_redis_host,
        port=settings.cache_redis_port,
        socket_connect_timeout=1,
        socket_timeout=1,
        decode_responses=True,
    )
