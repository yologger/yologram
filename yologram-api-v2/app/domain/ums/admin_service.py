import bcrypt
from sqlalchemy.orm import Session

from app.core.response import ApiEnvelopPage

from app.core.exception import (
    AdminUserDuplicateException,
    AdminUserNotFoundException,
    AdminUserSelfDeleteException,
    AuthWrongPasswordException,
)
from app.domain.ums.admin_jwt_util import create_admin_token
from app.domain.ums.admin_schema import (
    AdminAuthData,
    AdminLoginRequest,
    AdminLoginResponse,
    AdminUserCreateRequest,
    AdminUserCreateResponse,
    AdminUserResponse,
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

    def delete(self, auth_data: AdminAuthData, id: int) -> None:
        """hard delete — 자기 자신 삭제는 차단 (마지막 어드민 잠금 사고 방지)"""
        if auth_data.uid == id:
            raise AdminUserSelfDeleteException()

        admin = self.repository.find_by_id(id)
        if not admin:
            raise AdminUserNotFoundException()

        self.repository.delete(admin)

    def login(self, request: AdminLoginRequest) -> AdminLoginResponse:
        admin = self.repository.find_by_email(request.email)
        if not admin:
            raise AdminUserNotFoundException()

        if not bcrypt.checkpw(request.password.encode("utf-8"), admin.password.encode("utf-8")):
            raise AuthWrongPasswordException()

        access_token = create_admin_token(admin.id)

        return AdminLoginResponse(
            uid=admin.id,
            access_token=access_token,
            email=admin.email,
            name=admin.name,
        )

    def validate_token(self, auth_data: AdminAuthData) -> AdminValidateTokenResponse:
        admin = self.repository.find_by_id(auth_data.uid)
        if not admin:
            raise AdminUserNotFoundException()

        return AdminValidateTokenResponse(
            uid=admin.id,
            email=admin.email,
            name=admin.name,
        )

    def logout(self, auth_data: AdminAuthData) -> None:
        pass
