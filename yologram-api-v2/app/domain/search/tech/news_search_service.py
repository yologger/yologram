import logging

from sqlalchemy.orm import Session

from app.config.settings import get_settings
from app.core.exception import (
    BlankSearchKeywordException,
    SearchPageTooDeepException,
    SearchUnavailableException,
)
from app.core.response import ApiEnvelopPage
from app.domain.news.tech.schema import TechNewsResponse
from app.domain.search.tech.model import TechSearchSort
from app.domain.search.tech.news_document import TechNewsDocument
from app.domain.search.tech.repository.tech_news_search_repository import TechNewsSearchRepository
from app.infra.client.cms.cms_api_client import CmsApiClient, LocalCmsApiClient

logger = logging.getLogger(__name__)

# 목록 API와 같은 상한
MAX_PAGE_SIZE = 50

# OpenSearch index.max_result_window 기본값 — 인덱스 설정을 올리면 함께 올려야 한다
MAX_RESULT_WINDOW = 10_000


class TechNewsSearchService:
    """
    뉴스 검색 (api-v1 TechNewsSearchService 미러) —
    OpenSearch에서 문서를 찾고 목록 API와 같은 스키마로 응답한다.

    색인에 없는 값은 카테고리 라벨뿐이라 여기서 채운다 — 색인에는 id만 있고
    이름은 tech_category 마스터에서 바뀔 수 있다(이름을 색인하면 변경 때마다 재색인이 필요하다).
    게시글 검색이 닉네임을 ums에서 채우는 것과 같은 구조이고, 왕복은 건수와 무관하게 고정이다
    (OpenSearch 1 + 카테고리 1).

    페이징은 offset이다 — 커서로는 총건수·페이지 번호를 만들 수 없고 검색은 그 둘이 필요하다.
    """

    def __init__(
        self,
        db: Session,
        search_repository: TechNewsSearchRepository | None = None,
        cms_api_client: CmsApiClient | None = None,
    ):
        self.search_repository = search_repository or TechNewsSearchRepository()
        # 타 도메인(cms) 접근은 infra/client 경유 — 라벨 해석용 (목록 서비스와 동일)
        self.cms_api_client: CmsApiClient = cms_api_client or LocalCmsApiClient(db)

    def search(
        self,
        keyword: str,
        page: int,
        size: int,
        sort: TechSearchSort,
    ) -> ApiEnvelopPage[TechNewsResponse]:
        # 설정이 없는 환경(로컬·테스트 기본)에서는 엔진에 붙지 않고 503으로 끊는다
        if not get_settings().opensearch_main_enabled:
            raise SearchUnavailableException()

        trimmed = keyword.strip()
        if not trimmed:
            raise BlankSearchKeywordException()

        page_number = max(0, page)
        page_size = max(1, min(size, MAX_PAGE_SIZE))
        from_ = page_number * page_size

        # max_result_window 초과는 엔진이 예외를 내므로 미리 400으로 끊는다
        if from_ + page_size > MAX_RESULT_WINDOW:
            raise SearchPageTooDeepException()

        result = self.search_repository.search(trimmed, from_=from_, size=page_size, sort=sort)

        # 카테고리 라벨 배치 해석 (N+1 회피 — 뉴스 목록 API와 같은 방식)
        category_ids = {cid for doc in result.documents for cid in doc.category_ids}
        name_by_id = self.cms_api_client.find_category_names(category_ids)

        data = [self._to_response(doc, name_by_id) for doc in result.documents]

        total_pages = 0 if result.total_count == 0 else (result.total_count + page_size - 1) // page_size
        return ApiEnvelopPage(
            data=data,
            page=page_number,
            size=page_size,
            total_pages=total_pages,
            total_count=result.total_count,
            first=(page_number == 0),
            last=(total_pages == 0 or page_number >= total_pages - 1),
        )

    @staticmethod
    def _to_response(doc: TechNewsDocument, name_by_id: dict[int, str]) -> TechNewsResponse:
        return TechNewsResponse(
            id=doc.id,
            title=doc.title,
            summary=doc.summary,
            link=doc.link,
            source_name=doc.source_name,
            # 삭제된 카테고리 매핑은 라벨 표시에서 제외 (목록 API와 같은 처리)
            categories=[name_by_id[cid] for cid in doc.category_ids if cid in name_by_id],
            published_at=doc.published_at,
        )
