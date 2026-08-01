from fastapi import APIRouter, Depends, Response, status
from sqlalchemy.orm import Session

from app.config.database import get_db
from app.core.response import ApiEnvelop
from app.domain.news.tech.admin_schema import (
    AdminTechNewsSourceCreateRequest,
    AdminTechNewsSourceUpdateRequest,
)
from app.domain.news.tech.admin_service import AdminTechNewsSourceService
from app.domain.ums.admin_auth_dependency import get_authenticated_admin
from app.domain.ums.admin_schema import AdminAuthData

# 어드민 테크 뉴스 소스 관리 API — worker가 수집하는 RSS 피드 소스(tech_news_source) CRUD.
# 경로는 도메인 뒤 admin 세그먼트 규칙(/news/admin/...) 적용 (api-v1 미러).
router = APIRouter(prefix="/api/v2/news/admin/tech/sources", tags=["AdminTechNewsSource"])


@router.get(
    "",
    response_model=ApiEnvelop,
    summary="테크 뉴스 소스 목록 조회",
    description="전체 소스를 id 오름차순으로 조회 (어드민 토큰 필요)",
    responses={
        200: {"description": "조회 성공"},
        401: {"description": "인증 실패 (어드민 토큰 없음/만료/유효하지 않음)"},
    },
)
def get_sources(
    auth_data: AdminAuthData = Depends(get_authenticated_admin),
    db: Session = Depends(get_db),
):
    service = AdminTechNewsSourceService(db)
    return ApiEnvelop(data=service.get_sources())


@router.post(
    "",
    response_model=ApiEnvelop,
    status_code=status.HTTP_201_CREATED,
    summary="테크 뉴스 소스 생성",
    description="RSS 피드 소스를 추가 (어드민 토큰 필요)",
    responses={
        201: {"description": "생성 성공"},
        400: {"description": "입력값 검증 실패 (VALIDATION_ERROR)"},
        401: {"description": "인증 실패 (어드민 토큰 없음/만료/유효하지 않음)"},
        409: {"description": "이미 등록된 뉴스 소스 URL (NEWS_SOURCE_DUPLICATE)"},
    },
)
def create(
    request: AdminTechNewsSourceCreateRequest,
    auth_data: AdminAuthData = Depends(get_authenticated_admin),
    db: Session = Depends(get_db),
):
    service = AdminTechNewsSourceService(db)
    return ApiEnvelop(data=service.create(request))


@router.patch(
    "/{id}",
    response_model=ApiEnvelop,
    summary="테크 뉴스 소스 수정",
    description="널 필드는 미변경 (부분 갱신, 어드민 토큰 필요)",
    responses={
        200: {"description": "수정 성공"},
        400: {"description": "입력값 검증 실패 (VALIDATION_ERROR)"},
        401: {"description": "인증 실패 (어드민 토큰 없음/만료/유효하지 않음)"},
        404: {"description": "뉴스 소스를 찾을 수 없음 (NEWS_SOURCE_NOT_FOUND)"},
        409: {"description": "이미 등록된 뉴스 소스 URL (NEWS_SOURCE_DUPLICATE)"},
    },
)
def update(
    id: int,
    request: AdminTechNewsSourceUpdateRequest,
    auth_data: AdminAuthData = Depends(get_authenticated_admin),
    db: Session = Depends(get_db),
):
    service = AdminTechNewsSourceService(db)
    return ApiEnvelop(data=service.update(id, request))


@router.delete(
    "/{id}",
    status_code=status.HTTP_204_NO_CONTENT,
    summary="테크 뉴스 소스 삭제",
    description="hard delete — 수집 중지는 isActive=false 사용 권장 (어드민 토큰 필요)",
    responses={
        204: {"description": "삭제 성공"},
        401: {"description": "인증 실패 (어드민 토큰 없음/만료/유효하지 않음)"},
        404: {"description": "뉴스 소스를 찾을 수 없음 (NEWS_SOURCE_NOT_FOUND)"},
    },
)
def delete(
    id: int,
    auth_data: AdminAuthData = Depends(get_authenticated_admin),
    db: Session = Depends(get_db),
):
    service = AdminTechNewsSourceService(db)
    service.delete(id)
    return Response(status_code=status.HTTP_204_NO_CONTENT)
