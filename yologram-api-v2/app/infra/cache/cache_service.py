from typing import Any, Protocol

from app.infra.cache.cache import Cache


class CacheService(Protocol):
    """캐시 연산 추상화 (api-v1 CacheService 미러). 구현은 실패를 삼키고 미스로 처리해 호출부가 DB로 폴백하게 한다."""

    def get_or_null(self, cache: Cache) -> Any | None:
        ...

    def get_all_as_map(self, caches: list[Cache]) -> dict[str, Any]:
        """
        배치 조회 (MGET) — key→value 맵으로 반환하고 미스 키는 제외한다.
        부분 히트 판별이 필요한 배치 캐시(cache-aside)용.
        """
        ...

    def set(self, cache: Cache, value: Any) -> None:
        ...

    def set_all(self, entries: dict[Cache, Any]) -> None:
        ...

    def delete_all(self, *caches: Cache) -> None:
        ...
