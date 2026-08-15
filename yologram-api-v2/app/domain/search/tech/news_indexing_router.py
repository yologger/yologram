from fastapi import APIRouter, BackgroundTasks, Depends, status
from sqlalchemy.orm import Session

from app.config.database import get_db
from app.domain.search.tech.news_indexing_service import AdminTechNewsIndexingService
from app.domain.ums.admin_auth_dependency import get_authenticated_admin
from app.domain.ums.admin_schema import AdminAuthData

# 어드민 뉴스 인덱싱 API (api-v1 AdminTechNewsIndexingResource 미러) —
# 게시글 인덱싱과 같은 구조·같은 큐를 쓰고 target만 다르다.
#
# 게시글과 달리 평상시 색인은 이 API가 아니라 worker의 요약 배치가 직접 한다(요약 완료 직후 색인).
# 여기는 그 실시간 경로가 놓친 구간을 메우는 보정 도구다 — 색인 실패로 빠진 건,
# 매핑 변경 후 재색인, 검색을 나중에 켠 경우의 과거 데이터.
#
# 세 엔드포인트 모두 SQS에 작업을 넣고 즉시 202로 응답한다 — 실제 인덱싱은 worker가 비동기로 수행한다.
router = APIRouter(prefix="/api/v2/search/admin/tech/news/indexing", tags=["AdminTechNewsIndexing"])


@router.put(
    "",
    status_code=status.HTTP_202_ACCEPTED,
    summary="뉴스 전체 인덱싱",
    description="1 ~ max(id) 범위를 20건 단위로 쪼개 SQS에 발행한다. 발행 자체가 백그라운드라 즉시 202로 응답하고, 진행 상황은 SQS 큐 깊이로 확인한다. 실제 인덱싱은 worker가 수행 (어드민 토큰 필요)",
    responses={
        202: {"description": "인덱싱 작업 발행 완료"},
        401: {"description": "인증 실패 (어드민 토큰 없음/만료/유효하지 않음)"},
    },
)
def full_index(
    background_tasks: BackgroundTasks,
    auth_data: AdminAuthData = Depends(get_authenticated_admin),
    db: Session = Depends(get_db),
):
    service = AdminTechNewsIndexingService(db)
    background_tasks.add_task(service.full_index_in_background)


@router.put(
    "/{id}",
    status_code=status.HTTP_202_ACCEPTED,
    summary="뉴스 단건 인덱싱",
    description="해당 id 하나만 인덱싱 (from == to로 범위 인덱싱과 같은 경로) (어드민 토큰 필요)",
    responses={
        202: {"description": "인덱싱 작업 발행 완료"},
        401: {"description": "인증 실패 (어드민 토큰 없음/만료/유효하지 않음)"},
    },
)
def index(
    id: int,
    auth_data: AdminAuthData = Depends(get_authenticated_admin),
    db: Session = Depends(get_db),
):
    AdminTechNewsIndexingService(db).index(id)


@router.put(
    "/{from_id}/{to_id}",
    status_code=status.HTTP_202_ACCEPTED,
    summary="뉴스 범위 인덱싱",
    description="from ~ to 범위를 20건 단위로 쪼개 SQS에 발행 (어드민 토큰 필요)",
    responses={
        202: {"description": "인덱싱 작업 발행 완료"},
        400: {"description": "from > to 또는 from < 1"},
        401: {"description": "인증 실패 (어드민 토큰 없음/만료/유효하지 않음)"},
    },
)
def index_range(
    from_id: int,
    to_id: int,
    auth_data: AdminAuthData = Depends(get_authenticated_admin),
    db: Session = Depends(get_db),
):
    AdminTechNewsIndexingService(db).index_range(from_id=from_id, to_id=to_id)
