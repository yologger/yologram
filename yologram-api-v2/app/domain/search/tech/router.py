from fastapi import APIRouter, BackgroundTasks, Depends, status
from sqlalchemy.orm import Session

from app.config.database import get_db
from app.domain.search.tech.service import AdminTechPostIndexingService
from app.domain.ums.admin_auth_dependency import get_authenticated_admin
from app.domain.ums.admin_schema import AdminAuthData

# 어드민 게시글 인덱싱 API (api-v1 AdminTechPostIndexingResource 미러) — 검색 인덱스 재구축 조작.
# 경로는 도메인 뒤 admin 세그먼트 규칙(/search/admin/...) + 대상 뒤 조작 세그먼트(indexing) 적용.
# /posts에 PUT을 걸면 게시글 수정으로 읽히고 검색 API(/search/tech/posts)와 성격이 뒤섞인다.
#
# 세 엔드포인트 모두 SQS에 작업을 넣고 즉시 202로 응답한다 — 실제 인덱싱은 worker가 비동기로 수행한다.
# 인덱싱은 운영 조작이라 어드민 토큰을 요구한다(공개로 열면 누구나 풀 인덱싱을 유발해 부하를 준다).
router = APIRouter(prefix="/api/v2/search/admin/tech/posts/indexing", tags=["AdminTechPostIndexing"])


@router.put(
    "",
    status_code=status.HTTP_202_ACCEPTED,
    summary="게시글 전체 인덱싱",
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
    service = AdminTechPostIndexingService(db)
    background_tasks.add_task(service.full_index_in_background)


@router.put(
    "/{id}",
    status_code=status.HTTP_202_ACCEPTED,
    summary="게시글 단건 인덱싱",
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
    AdminTechPostIndexingService(db).index(id)


@router.put(
    "/{from_id}/{to_id}",
    status_code=status.HTTP_202_ACCEPTED,
    summary="게시글 범위 인덱싱",
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
    AdminTechPostIndexingService(db).index_range(from_id=from_id, to_id=to_id)
