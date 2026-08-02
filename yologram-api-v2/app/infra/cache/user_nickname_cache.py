from collections.abc import Callable, Collection

from app.infra.cache.cache import Cache
from app.infra.cache.cache_service import CacheService
from app.infra.cache.redis_cache_service import RedisCacheService


class UserNicknameCache:
    """
    유저 닉네임 cache-aside 공용 컴포넌트 (api-v1 UserNicknameCache 미러).
    DB 조회는 loader 콜러블로 주입받아 도메인 경계(리포지토리 소유)는 호출부(infra/client)에 남긴다.
    """

    def __init__(self, cache_service: CacheService | None = None):
        self.cache_service: CacheService = cache_service or RedisCacheService()

    def get_nickname(self, uid: int, loader: Callable[[int], str | None]) -> str | None:
        """단건 cache-aside: 히트 시 반환, 미스 시 loader → 결과 캐시 (None이면 미캐시)."""
        cache = Cache.user_nickname(uid)
        cached = self.cache_service.get_or_null(cache)
        if cached is not None:
            return cached

        loaded = loader(uid)
        if loaded is None:
            return None
        self.cache_service.set(cache, loaded)
        return loaded

    def get_nicknames(
        self, uids: Collection[int], loader: Callable[[Collection[int]], dict[int, str]]
    ) -> dict[int, str]:
        """
        배치 cache-aside: get_all_as_map으로 히트 분리 → 미스 uid만 loader(IN 조회) → set_all로 채움 → 병합.
        Redis 장애 시 get_all_as_map이 빈 맵(전체 미스)을 돌려주므로 자연스럽게 전체 DB 폴백된다.
        """
        if not uids:
            return {}

        distinct_uids = set(uids)
        cache_by_uid = {uid: Cache.user_nickname(uid) for uid in distinct_uids}
        hit_by_key = self.cache_service.get_all_as_map(list(cache_by_uid.values()))
        hits = {
            uid: hit_by_key[cache.key]
            for uid, cache in cache_by_uid.items()
            if cache.key in hit_by_key
        }

        missed_uids = distinct_uids - hits.keys()
        if not missed_uids:
            return hits

        loaded = loader(missed_uids)
        if loaded:
            self.cache_service.set_all(
                {Cache.user_nickname(uid): nickname for uid, nickname in loaded.items()}
            )
        return {**hits, **loaded}
