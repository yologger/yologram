import logging

from sqlalchemy.orm import Session

from app.config.settings import get_settings
from app.core.exception import (
    BlankSearchKeywordException,
    SearchPageTooDeepException,
    SearchUnavailableException,
)
from app.core.response import ApiEnvelopPage
from app.domain.pms.tech.repository import TechPostRepository
from app.domain.pms.tech.schema import PostAuthor, PostMetrics, PostSummaryResponse
from app.domain.search.tech.document import TechPostDocument
from app.domain.search.tech.model import TechPostSearchSort
from app.domain.search.tech.repository.tech_post_search_repository import TechPostSearchRepository
from app.infra.client.ums.ums_api_client import LocalUmsApiClient, UmsApiClient

logger = logging.getLogger(__name__)

# 목록 API와 같은 상한
MAX_PAGE_SIZE = 50

# OpenSearch index.max_result_window 기본값 — 인덱스 설정을 올리면 함께 올려야 한다
MAX_RESULT_WINDOW = 10_000


class TechPostSearchService:
    """
    게시글 검색 (api-v1 TechPostSearchService 미러) —
    OpenSearch에서 문서를 찾고 목록 API와 같은 스키마로 응답한다.

    색인 문서에 없는 두 값은 여기서 채운다 —
      닉네임: ums 배치 조회(색인에 넣으면 닉네임 변경 때마다 재색인이 필요하다)
      likedByMe: 개인화 값이라 색인 대상이 아니다(선택 인증, 비로그인은 False)
    왕복은 결과 건수와 무관하게 고정이다(OpenSearch 1 + 좋아요 1 + 닉네임 1).

    페이징은 offset이다 — 커서로는 총건수·페이지 번호를 만들 수 없고 검색은 그 둘이 필요하다.
    """

    def __init__(
        self,
        db: Session,
        search_repository: TechPostSearchRepository | None = None,
        ums_api_client: UmsApiClient | None = None,
    ):
        self.post_repository = TechPostRepository(db)
        self.search_repository = search_repository or TechPostSearchRepository()
        # UmsApiClient는 Protocol이라 인스턴스화할 수 없다 — 구현체를 쓴다 (목록 서비스와 동일)
        self.ums_api_client: UmsApiClient = ums_api_client or LocalUmsApiClient(db)

    def search(
        self,
        keyword: str,
        page: int,
        size: int,
        sort: TechPostSearchSort,
        viewer_uid: int | None = None,
    ) -> ApiEnvelopPage[PostSummaryResponse]:
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

        # 닉네임·likedByMe 배치 조회 (N+1 회피 — 목록 API와 같은 방식)
        nicknames = self.ums_api_client.find_nicknames([doc.uid for doc in result.documents])
        liked_post_ids = self._find_liked_post_ids(viewer_uid, [doc.id for doc in result.documents])

        data = [
            self._to_summary(doc, nicknames.get(doc.uid), doc.id in liked_post_ids)
            for doc in result.documents
        ]

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
    def _to_summary(
        doc: TechPostDocument, nickname: str | None, liked_by_me: bool
    ) -> PostSummaryResponse:
        return PostSummaryResponse(
            id=doc.id,
            author=PostAuthor(uid=doc.uid, nickname=nickname),
            title=doc.title,
            content=doc.content,
            category_ids=doc.category_ids,
            metrics=PostMetrics(
                comment_count=doc.metrics.comment_count,
                like_count=doc.metrics.like_count,
                view_count=doc.metrics.view_count,
                liked_by_me=liked_by_me,
            ),
            created_at=doc.created_at,
        )

    def _find_liked_post_ids(self, viewer_uid: int | None, post_ids: list[int]) -> set[int]:
        if viewer_uid is None or not post_ids:
            return set()
        return self.post_repository.find_liked_post_ids(viewer_uid, post_ids)
