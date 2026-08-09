from collections.abc import Callable

from app.core.response import ApiEnvelopCursorPage
from app.domain.news.tech.schema import TechNewsResponse
from app.infra.cache.cache import Cache
from app.infra.cache.cache_service import CacheService
from app.infra.cache.redis_cache_service import RedisCacheService


class TechNewsFirstPageCache:
    """
    테크 뉴스 첫 페이지 cache-aside 공용 컴포넌트 (api-v1 TechNewsFirstPageCache 미러).
    조회는 페이지 키 GET 1회 — 버전 키 간접층 없음. 무효화는 worker가 새 뉴스 요약 시
    첫 페이지 키를 전수 열거해 UNLINK(worker TechNewsFirstPageCacheInvalidator와 키 계약).
    TTL 3분은 삭제 누락·레이스(삭제 직후 옛 목록 SET 부활) 대비 보험이자 낡음의 상한.
    저장 값은 응답 envelope JSON(camelCase, datetime ISO) — api-v1 Jackson이 직렬화한
    ApiEnvelopCursorPage<TechNewsResponse>와 동일 구조라 v1↔v2 캐시 상호 호환.
    DB 조회는 loader 콜러블로 주입받아 도메인 경계(리포지토리 소유)는 호출부에 남긴다.
    """

    def __init__(self, cache_service: CacheService | None = None):
        self.cache_service: CacheService = cache_service or RedisCacheService()

    def get(
        self,
        category_id: int | None,
        size: int,
        loader: Callable[[], ApiEnvelopCursorPage[TechNewsResponse]],
    ) -> ApiEnvelopCursorPage[TechNewsResponse]:
        """첫 페이지 cache-aside: 페이지 키 조회 → 히트 복원 / 미스면 loader → 저장."""
        page_cache = Cache.tech_news_first_page(category_id, size)
        cached = self.cache_service.get_or_null(page_cache)
        if cached is not None:
            return self._from_cached(cached)

        loaded = loader()
        self.cache_service.set(page_cache, self._to_cacheable(loaded))
        return loaded

    @staticmethod
    def _to_cacheable(page: ApiEnvelopCursorPage[TechNewsResponse]) -> dict:
        # camelCase(by_alias) + datetime ISO 초 단위(mode="json") — v1 LocalDateTime 직렬화와 일치.
        # null nextCursor는 envelope의 model_serializer가 생략 (v1 @JsonInclude NON_NULL 정합)
        return page.model_dump(by_alias=True, mode="json")

    @staticmethod
    def _from_cached(cached: dict) -> ApiEnvelopCursorPage[TechNewsResponse]:
        data = [TechNewsResponse.model_validate(item) for item in cached.get("data", [])]
        # nextCursor 미존재(null 생략 규약) → None
        return ApiEnvelopCursorPage(data=data, next_cursor=cached.get("nextCursor"))
