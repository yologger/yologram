from sqlalchemy.orm import Session

from app.core.response import ApiEnvelopCursorPage
from app.domain.news.tech.cursor import TechNewsCursor
from app.domain.news.tech.repository import TechNewsCategoryMappingRepository, TechNewsRepository
from app.domain.news.tech.schema import TechNewsResponse
from app.domain.cms.tech.repository import TechCategoryRepository

MAX_PAGE_SIZE = 50


class TechNewsService:

    def __init__(self, db: Session):
        self.news_repository = TechNewsRepository(db)
        self.mapping_repository = TechNewsCategoryMappingRepository(db)
        self.category_repository = TechCategoryRepository(db)

    def get_news_by_cursor(
        self, category_id: int | None, cursor: str | None, size: int
    ) -> ApiEnvelopCursorPage[TechNewsResponse]:
        """
        테크 뉴스 발행순 피드 (keyset cursor).
        worker가 요약을 마친(SUMMARIZED) 뉴스만 노출 — COLLECTED는 몇 분 내 요약되는 일시 상태, FAILED는 제외.
        """
        page_size = max(1, min(size, MAX_PAGE_SIZE))
        decoded_cursor = TechNewsCursor.decode(cursor) if cursor else None

        news = self.news_repository.find_summarized_news(category_id, decoded_cursor, page_size)

        # 카테고리 배치 조회 후 tech_category 마스터에서 라벨 해석 (N+1 회피 — 게시판·api-v1 패턴)
        mappings = self.mapping_repository.find_by_news_ids([n.id for n in news])
        name_by_id = {
            c.id: c.name
            for c in self.category_repository.find_by_ids(list({m.category_id for m in mappings}))
        }
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
