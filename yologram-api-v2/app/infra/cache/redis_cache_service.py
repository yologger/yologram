import json
import logging
from typing import Any

import redis

from app.config.redis import get_redis_client
from app.infra.cache.cache import Cache

logger = logging.getLogger(__name__)


class RedisCacheService:
    """
    redis-py + JSON 기반 CacheService 구현 (api-v1 RedisCacheService 미러).
    값은 JSON 직렬화로 저장 — api-v1(Jackson)과 동일 표현이라 캐시 데이터 상호 호환.
    캐시는 보조 수단이므로 전 연산 try/except — Redis 장애 시 로그만 남기고
    미스(None/빈 결과)로 처리해 호출부가 DB로 폴백하게 한다.
    """

    def __init__(self, client: redis.Redis | None = None):
        self.client = client or get_redis_client()

    def get_or_null(self, cache: Cache) -> Any | None:
        try:
            raw = self.client.get(cache.key)
            if raw is None or raw == "":
                return None
            return json.loads(raw)
        except Exception:
            logger.error("unexpected error occurred while reading data from redis", exc_info=True)
            return None

    def get_all_as_map(self, caches: list[Cache]) -> dict[str, Any]:
        if not caches:
            return {}
        keys = [c.key for c in caches]
        try:
            # MGET은 키 순서대로 값/None을 돌려주므로 키와 zip해 미스(None)를 제외한 맵 구성
            raws = self.client.mget(keys)
            return {
                key: json.loads(raw)
                for key, raw in zip(keys, raws)
                if raw is not None and raw != ""
            }
        except Exception:
            logger.error("unexpected error occurred while reading data from redis", exc_info=True)
            return {}  # 실패는 전체 미스로 취급 → 호출부 DB 폴백

    def set(self, cache: Cache, value: Any) -> None:
        try:
            self.client.set(cache.key, self._to_json(value), ex=int(cache.ttl.total_seconds()))
        except Exception:
            logger.error("unexpected error occurred while saving data to redis", exc_info=True)

    def set_all(self, entries: dict[Cache, Any]) -> None:
        try:
            for cache, value in entries.items():
                self.client.set(cache.key, self._to_json(value), ex=int(cache.ttl.total_seconds()))
        except Exception:
            logger.error("unexpected error occurred while saving data to redis", exc_info=True)

    @staticmethod
    def _to_json(value: Any) -> str:
        # ensure_ascii=False — 한글을 이스케이프 없이 저장 (api-v1 Jackson 표현과 바이트 수준 호환)
        return json.dumps(value, ensure_ascii=False)

    def delete_all(self, *caches: Cache) -> None:
        if not caches:
            return
        try:
            self.client.delete(*[c.key for c in caches])
        except Exception:
            logger.error("unexpected error occurred while deleting data from redis", exc_info=True)
