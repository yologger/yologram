import json
from datetime import datetime, timedelta
from unittest.mock import MagicMock

from app.core.response import ApiEnvelopCursorPage
from app.domain.news.tech.schema import TechNewsResponse
from app.infra.cache.cache import Cache
from app.infra.cache.tech_news_first_page_cache import TechNewsFirstPageCache


def _response(news_id: int, published_at: datetime = datetime(2026, 7, 18, 14, 23, 50)) -> TechNewsResponse:
    return TechNewsResponse(
        id=news_id,
        title=f"제목 {news_id}",
        summary=f"요약 {news_id}",
        link=f"https://a/{news_id}",
        source_name="테크 블로그",
        categories=["Backend"],
        published_at=published_at,
    )


def _page(*responses: TechNewsResponse, next_cursor: str | None = None) -> ApiEnvelopCursorPage:
    return ApiEnvelopCursorPage(data=list(responses), next_cursor=next_cursor)


def _cache(page_value=None) -> tuple[TechNewsFirstPageCache, MagicMock]:
    """페이지 키 조회를 스텁한 캐시 컴포넌트 구성 (조회는 페이지 키 GET 1회 — 버전 키 없음)."""
    cache_service = MagicMock()
    cache_service.get_or_null.return_value = page_value
    return TechNewsFirstPageCache(cache_service), cache_service


class TestTechNewsFirstPageCache:

    def test_히트면_loader를_호출하지_않고_복원해_반환한다(self):
        stored = TechNewsFirstPageCache._to_cacheable(_page(_response(1), next_cursor="커서"))
        cache, cache_service = _cache(page_value=stored)
        loader = MagicMock()

        result = cache.get(category_id=None, size=20, loader=loader)

        assert result.data[0].title == "제목 1"
        assert result.next_cursor == "커서"
        loader.assert_not_called()
        cache_service.set.assert_not_called()

    def test_미스면_loader_결과를_camelCase_envelope로_저장하고_반환한다(self):
        cache, cache_service = _cache(page_value=None)
        page = _page(_response(1), next_cursor="커서")
        loader = MagicMock(return_value=page)

        result = cache.get(category_id=10, size=20, loader=loader)

        assert result is page
        loader.assert_called_once_with()
        (set_cache, set_value), _ = cache_service.set.call_args
        # 키 스킴 고정 검증: news:tech:v1:first-page:{category|all}:{size} — worker UNLINK 열거와의 계약
        assert set_cache == Cache.tech_news_first_page(10, 20)
        assert set_cache.key == "news:tech:v1:first-page:10:20"
        # 저장 값은 camelCase envelope + datetime ISO 초 단위 (api-v1 Jackson 호환)
        assert set_value == {
            "data": [
                {
                    "id": 1,
                    "title": "제목 1",
                    "summary": "요약 1",
                    "link": "https://a/1",
                    "sourceName": "테크 블로그",
                    "categories": ["Backend"],
                    "publishedAt": "2026-07-18T14:23:50",
                }
            ],
            "nextCursor": "커서",
        }

    def test_category_id가_None이면_all_세그먼트를_쓴다(self):
        cache, cache_service = _cache(page_value=None)

        cache.get(category_id=None, size=30, loader=MagicMock(return_value=_page()))

        page_key = cache_service.get_or_null.call_args_list[-1].args[0].key
        assert page_key == "news:tech:v1:first-page:all:30"

    def test_TTL은_3분이다(self):
        # 삭제 누락·레이스 부활 대비 보험 계약 (worker Invalidator 주석과 정합)
        assert Cache.tech_news_first_page(None, 20).ttl == timedelta(minutes=3)

    def test_Redis_장애_전체_미스_시_loader_DB_폴백으로_기능_무손상(self):
        # RedisCacheService는 장애 시 get_or_null이 None — 미스와 동일 경로
        cache, cache_service = _cache(page_value=None)
        page = _page(_response(1))

        result = cache.get(category_id=None, size=20, loader=MagicMock(return_value=page))

        assert result is page

    def test_저장값_round_trip__직렬화_복원_후_동일_응답(self):
        page = _page(_response(1), _response(2), next_cursor="다음커서")

        dumped = TechNewsFirstPageCache._to_cacheable(page)
        # RedisCacheService 저장·조회와 동일하게 JSON 문자열 왕복
        restored = TechNewsFirstPageCache._from_cached(json.loads(json.dumps(dumped, ensure_ascii=False)))

        assert restored.data == page.data
        assert restored.next_cursor == page.next_cursor
        assert restored.data[0].published_at == datetime(2026, 7, 18, 14, 23, 50)
        # datetime은 초 단위 ISO — v1 LocalDateTime 직렬화와 일치
        assert dumped["data"][0]["publishedAt"] == "2026-07-18T14:23:50"

    def test_round_trip__nextCursor가_None이면_필드_생략_후_None으로_복원(self):
        page = _page(_response(1), next_cursor=None)

        dumped = TechNewsFirstPageCache._to_cacheable(page)
        restored = TechNewsFirstPageCache._from_cached(dumped)

        assert "nextCursor" not in dumped  # v1 @JsonInclude NON_NULL 정합 — null 필드 생략
        assert restored.next_cursor is None
        assert restored.data == page.data
