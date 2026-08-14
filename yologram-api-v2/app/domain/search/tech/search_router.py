from fastapi import APIRouter, Depends, Query
from sqlalchemy.orm import Session

from app.config.database import get_db
from app.core.response import ApiEnvelopPage
from app.domain.pms.tech.schema import PostSummaryResponse
from app.domain.search.tech.model import TechPostSearchSort
from app.domain.search.tech.search_service import TechPostSearchService
from app.domain.ums.auth_dependency import get_optional_authenticated_user
from app.domain.ums.auth_schema import AuthData

# 테크 게시글 검색 API (api-v1 TechPostSearchResource 미러) — 공개 다건 탐색이라 search 도메인이 담당한다
# (단건·쓰기·"내 것"은 pms. docs/todos.md의 pms vs search 호출 기준).
#
# 페이징은 offset(page/size)이다 — 프론트가 페이지 네비게이션을 쓰고 총건수·페이지 수가 필요하다.
# 개인화(likedByMe)를 위해 선택 인증을 받는다: 헤더가 없으면 비로그인으로 처리하고 False를 준다.
router = APIRouter(prefix="/api/v2/search/tech/posts", tags=["TechPostSearch"])


@router.get(
    "",
    response_model=ApiEnvelopPage[PostSummaryResponse],
    response_model_by_alias=True,
    summary="게시글 검색",
    description="제목·본문을 형태소(nori) 기준으로 검색한다. 제목 가중치 2배. 로그인 시 likedByMe 포함",
    responses={
        200: {"description": "검색 성공"},
        400: {"description": "검색어 없음(BLANK_SEARCH_KEYWORD) 또는 조회 한계 초과(SEARCH_PAGE_TOO_DEEP)"},
        401: {"description": "인증 헤더가 있으나 토큰이 유효하지 않음"},
    },
)
def search_posts(
    q: str = Query(description="검색어", example="제미나이"),
    page: int = Query(default=0, description="페이지 번호 (0부터)"),
    size: int = Query(default=10, description="페이지 크기 (최대 50)"),
    sort: TechPostSearchSort = Query(default=TechPostSearchSort.RELEVANCE, description="정렬 기준"),
    auth_data: AuthData | None = Depends(get_optional_authenticated_user),
    db: Session = Depends(get_db),
):
    service = TechPostSearchService(db)
    return service.search(
        keyword=q,
        page=page,
        size=size,
        sort=sort,
        viewer_uid=auth_data.uid if auth_data else None,
    )
