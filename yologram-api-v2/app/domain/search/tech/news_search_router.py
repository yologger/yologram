from fastapi import APIRouter, Depends, Query
from sqlalchemy.orm import Session

from app.config.database import get_db
from app.core.response import ApiEnvelopPage
from app.domain.news.tech.schema import TechNewsResponse
from app.domain.search.tech.model import TechSearchSort
from app.domain.search.tech.news_search_service import TechNewsSearchService

# 테크 뉴스 검색 API (api-v1 TechNewsSearchResource 미러) — 공개 다건 탐색이라 search 도메인이 담당한다
# (목록·단건은 news 도메인. docs/todos.md의 호출 기준).
#
# 페이징은 offset(page/size)이다 — 프론트가 페이지 네비게이션을 쓰고 총건수·페이지 수가 필요하다
# (뉴스 목록 API는 무한스크롤이라 커서를 쓰지만 검색은 다르다).
#
# 게시글 검색과 달리 인증을 받지 않는다 — 뉴스에는 개인화 값(likedByMe 같은)이 없다.
router = APIRouter(prefix="/api/v2/search/tech/news", tags=["TechNewsSearch"])


@router.get(
    "",
    response_model=ApiEnvelopPage[TechNewsResponse],
    response_model_by_alias=True,
    summary="뉴스 검색",
    description="제목·요약을 형태소(nori) 기준으로 검색한다. 제목 가중치 2배. 최신순은 발행 시각(publishedAt) 기준",
    responses={
        200: {"description": "검색 성공"},
        400: {"description": "검색어 없음(BLANK_SEARCH_KEYWORD) 또는 조회 한계 초과(SEARCH_PAGE_TOO_DEEP)"},
        503: {"description": "검색 설정 없음 (SEARCH_UNAVAILABLE)"},
    },
)
def search_news(
    q: str = Query(description="검색어", example="마이그레이션"),
    page: int = Query(default=0, description="페이지 번호 (0부터)"),
    size: int = Query(default=10, description="페이지 크기 (최대 50)"),
    sort: TechSearchSort = Query(default=TechSearchSort.RELEVANCE, description="정렬 기준"),
    db: Session = Depends(get_db),
):
    service = TechNewsSearchService(db)
    return service.search(keyword=q, page=page, size=size, sort=sort)
