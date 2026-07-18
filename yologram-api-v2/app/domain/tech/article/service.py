from sqlalchemy.orm import Session

from app.core.response import ApiEnvelopCursorPage
from app.domain.tech.article.cursor import TechArticleCursor
from app.domain.tech.article.repository import TechArticleCategoryMappingRepository, TechArticleRepository
from app.domain.tech.article.schema import TechArticleResponse
from app.domain.tech.category.repository import TechCategoryRepository

MAX_PAGE_SIZE = 50


class TechArticleService:

    def __init__(self, db: Session):
        self.article_repository = TechArticleRepository(db)
        self.mapping_repository = TechArticleCategoryMappingRepository(db)
        self.category_repository = TechCategoryRepository(db)

    def get_articles_by_cursor(
        self, category_id: int | None, cursor: str | None, size: int
    ) -> ApiEnvelopCursorPage[TechArticleResponse]:
        """
        테크 아티클 발행순 피드 (keyset cursor).
        worker가 요약을 마친(SUMMARIZED) 아티클만 노출 — COLLECTED는 몇 분 내 요약되는 일시 상태, FAILED는 제외.
        """
        page_size = max(1, min(size, MAX_PAGE_SIZE))
        decoded_cursor = TechArticleCursor.decode(cursor) if cursor else None

        articles = self.article_repository.find_summarized_articles(category_id, decoded_cursor, page_size)

        # 카테고리 배치 조회 후 tech_category 마스터에서 라벨 해석 (N+1 회피 — 게시판·api-v1 패턴)
        mappings = self.mapping_repository.find_by_article_ids([a.id for a in articles])
        name_by_id = {
            c.id: c.name
            for c in self.category_repository.find_by_ids(list({m.category_id for m in mappings}))
        }
        categories_by_article: dict[int, list[str]] = {}
        for mapping in mappings:
            name = name_by_id.get(mapping.category_id)
            if name is not None:  # 삭제된 카테고리 매핑은 라벨 표시에서 제외
                categories_by_article.setdefault(mapping.article_id, []).append(name)

        data = [
            TechArticleResponse.from_article(article, categories_by_article.get(article.id, []))
            for article in articles
        ]

        next_cursor = (
            TechArticleCursor.encode(articles[-1].published_at, articles[-1].id) if articles else None
        )
        return ApiEnvelopCursorPage(data=data, next_cursor=next_cursor)
