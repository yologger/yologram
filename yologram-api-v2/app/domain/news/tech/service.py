from sqlalchemy.orm import Session

from app.core.response import ApiEnvelopCursorPage
from app.domain.news.tech.cursor import TechNewsCursor
from app.domain.news.tech.repository import TechNewsCategoryMappingRepository, TechNewsRepository
from app.domain.news.tech.schema import TechNewsResponse
from app.infra.cache.tech_news_first_page_cache import TechNewsFirstPageCache
from app.infra.client.cms.cms_api_client import CmsApiClient, LocalCmsApiClient

MAX_PAGE_SIZE = 50


class TechNewsService:

    def __init__(self, db: Session, first_page_cache: TechNewsFirstPageCache | None = None):
        self.news_repository = TechNewsRepository(db)
        self.mapping_repository = TechNewsCategoryMappingRepository(db)
        # 타 도메인(cms) 접근은 infra/client 경유 — 라벨 해석용
        self.cms_api_client: CmsApiClient = LocalCmsApiClient(db)
        self.first_page_cache = first_page_cache or TechNewsFirstPageCache()

    def get_news_by_cursor(
        self, category_id: int | None, cursor: str | None, size: int
    ) -> ApiEnvelopCursorPage[TechNewsResponse]:
        """
        테크 뉴스 발행순 피드 (keyset cursor).
        worker가 요약을 마친(SUMMARIZED) 뉴스만 노출 — COLLECTED는 몇 분 내 요약되는 일시 상태, FAILED는 제외.
        cursor 없는 첫 페이지 요청만 캐시 경유 — 트래픽 대부분이 첫 페이지, worker 버전 INCR로 즉시 무효화.
        """
        # 캐시 키 생성 전에 size 정규화 — 비정규 size로 인한 키 폭증 방지
        page_size = max(1, min(size, MAX_PAGE_SIZE))
        if not cursor:  # 기존 falsy 판정 유지 — 빈 문자열도 첫 페이지 취급
            return self.first_page_cache.get(
                category_id, page_size, lambda: self._load_news(category_id, None, page_size)
            )
        return self._load_news(category_id, TechNewsCursor.decode(cursor), page_size)

    def _load_news(
        self, category_id: int | None, decoded_cursor: TechNewsCursor | None, page_size: int
    ) -> ApiEnvelopCursorPage[TechNewsResponse]:
        """DB 조회 + 라벨 해석 (첫 페이지 캐시의 loader로도 사용)."""
        news = self.news_repository.find_summarized_news(category_id, decoded_cursor, page_size)

        # 카테고리 배치 조회 후 tech_category 마스터에서 라벨 해석 (N+1 회피 — 게시판·api-v1 패턴)
        mappings = self.mapping_repository.find_by_news_ids([n.id for n in news])
        name_by_id = self.cms_api_client.find_category_names({m.category_id for m in mappings})
        categories_by_news: dict[int, list[str]] = {}
        for mapping in mappings:
            name = name_by_id.get(mapping.category_id)
            if name is not None:  # 삭제된 카테고리 매핑은 라벨 표시에서 제외
                categories_by_news.setdefault(mapping.news_id, []).append(name)

        data = [
            TechNewsResponse.from_news(news_item, categories_by_news.get(news_item.id, []))
            for news_item in news
        ]

        next_cursor = (
            TechNewsCursor.encode(news[-1].published_at, news[-1].id) if news else None
        )
        return ApiEnvelopCursorPage(data=data, next_cursor=next_cursor)
