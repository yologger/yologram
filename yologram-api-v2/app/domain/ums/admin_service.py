import bcrypt
from sqlalchemy.orm import Session

from app.core.response import ApiEnvelopPage

from app.core.exception import (
    AdminRoleForbiddenException,
    AdminUserDuplicateException,
    AdminUserInactiveException,
    AdminUserNotFoundException,
    AdminUserOwnerImmutableException,
    AdminUserOwnerUndeletableException,
    AdminUserSelfDeleteException,
    AuthWrongPasswordException,
)
from app.domain.ums.enum import AdminUserRole, UserStatus
from app.domain.ums.admin_jwt_util import create_admin_token
from app.domain.ums.admin_schema import (
    AdminAuthData,
    AdminLoginRequest,
    AdminLoginResponse,
    AdminUserCreateRequest,
    AdminUserCreateResponse,
    AdminUserResponse,
    AdminUserStatusUpdateRequest,
    AdminValidateTokenResponse,
)
from app.domain.ums.model import AdminUser
from app.domain.ums.repository import AdminUserRepository


class AdminUserService:

    def __init__(self, db: Session):
        self.repository = AdminUserRepository(db)

    def create(self, request: AdminUserCreateRequest) -> AdminUserCreateResponse:
        existing = self.repository.find_by_email(request.email)
        if existing:
            raise AdminUserDuplicateException()

        hashed_password = bcrypt.hashpw(
            request.password.encode("utf-8"), bcrypt.gensalt()
        ).decode("utf-8")

        admin = AdminUser(
            email=request.email,
            name=request.name,
            password=hashed_password,
            # API 생성은 항상 ADMIN — OWNER 부여는 DB 직접 조작 전용 정책 (요청에 role 없음)
            role=AdminUserRole.ADMIN,
        )
        saved = self.repository.save(admin)

        return AdminUserCreateResponse(uid=saved.id)

    def get_admin_users(self, page: int, size: int) -> ApiEnvelopPage[AdminUserResponse]:
        """어드민 계정 목록 (offset 페이지네이션, id asc).
        page(0-based)·size(1~100) 검증은 라우터 Query 제약이 담당 (400 VALIDATION_ERROR)."""
        total_count = self.repository.count()
        admins = self.repository.find_page_order_by_id_asc(page * size, size)

        data = [
            AdminUserResponse(
                uid=admin.id,
                email=admin.email,
                name=admin.name,
                status=admin.status,
                role=admin.role,
                joined_date=admin.joined_date,
            )
            for admin in admins
        ]
        # 페이지 메타 산출 — pms offset(_to_page)·api-v1 ApiEnvelopPage와 동일 규칙
        total_pages = 0 if total_count == 0 else (total_count + size - 1) // size
        return ApiEnvelopPage(
            data=data,
            page=page,
            size=size,
            total_pages=total_pages,
            total_count=total_count,
            first=(page == 0),
            last=(total_pages == 0 or page >= total_pages - 1),
        )

    def update_status(
        self, auth_data: AdminAuthData, id: int, request: AdminUserStatusUpdateRequest
    ) -> AdminUserResponse:
        """어드민 활성/비활성 변경 — OWNER 전용. 검사 순서: 요청자 비-OWNER 403 → 대상 없음 404 → 대상 OWNER 400"""
        requester = self.repository.find_by_id(auth_data.uid)
        if not requester or requester.role != AdminUserRole.OWNER:
            raise AdminRoleForbiddenException()

        target = self.repository.find_by_id(id)
        if not target:
            raise AdminUserNotFoundException()

        # OWNER 계정은 상태 변경 불가 (비활성화로 인한 최상위 계정 잠금 방지)
        if target.role == AdminUserRole.OWNER:
            raise AdminUserOwnerImmutableException()

        target.status = request.status
        # modified_date(onupdate)가 응답에 반영되도록 즉시 flush
        saved = self.repository.save(target)
        return AdminUserResponse(
            uid=saved.id,
            email=saved.email,
            name=saved.name,
            status=saved.status,
            role=saved.role,
            joined_date=saved.joined_date,
        )

    def delete(self, auth_data: AdminAuthData, id: int) -> None:
        """hard delete — 자기 자신 삭제 차단 (마지막 어드민 잠금 사고 방지), OWNER 삭제 차단"""
        if auth_data.uid == id:
            raise AdminUserSelfDeleteException()

        admin = self.repository.find_by_id(id)
        if not admin:
            raise AdminUserNotFoundException()

        if admin.role == AdminUserRole.OWNER:
            raise AdminUserOwnerUndeletableException()

        self.repository.delete(admin)

    def login(self, request: AdminLoginRequest) -> AdminLoginResponse:
        admin = self.repository.find_by_email(request.email)
        if not admin:
            raise AdminUserNotFoundException()

        if not bcrypt.checkpw(request.password.encode("utf-8"), admin.password.encode("utf-8")):
            raise AuthWrongPasswordException()

        # 비활성 계정 차단 — 비밀번호 검증 후 체크 (계정 존재·비번 오류와 구분되는 403)
        if admin.status == UserStatus.INACTIVE:
            raise AdminUserInactiveException()

        access_token = create_admin_token(admin.id)

        return AdminLoginResponse(
            uid=admin.id,
            access_token=access_token,
            email=admin.email,
            name=admin.name,
            role=admin.role,
        )

    def validate_token(self, auth_data: AdminAuthData) -> AdminValidateTokenResponse:
        admin = self.repository.find_by_id(auth_data.uid)
        if not admin:
            raise AdminUserNotFoundException()

        # 발급된 토큰이 있어도 비활성화된 계정은 차단
        if admin.status == UserStatus.INACTIVE:
            raise AdminUserInactiveException()

        return AdminValidateTokenResponse(
            uid=admin.id,
            email=admin.email,
            name=admin.name,
            role=admin.role,
        )

    def logout(self, auth_data: AdminAuthData) -> None:
        pass
