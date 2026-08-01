from datetime import datetime

from pydantic import BaseModel, ConfigDict, EmailStr, Field, field_validator

from app.domain.ums.enum import AdminUserRole, UserStatus


class AdminUserCreateRequest(BaseModel):
    email: EmailStr
    name: str = Field(min_length=2, max_length=20)
    password: str = Field(min_length=8, max_length=20)


class AdminUserCreateResponse(BaseModel):
    uid: int


class AdminUserStatusUpdateRequest(BaseModel):
    """어드민 계정 활성/비활성 변경 요청 — ACTIVE/INACTIVE만 허용 (DELETED 등은 400)"""

    status: UserStatus = Field(description="변경할 상태 (ACTIVE 또는 INACTIVE만 허용)")

    @field_validator("status")
    @classmethod
    def status_allowed(cls, v: UserStatus) -> UserStatus:
        if v not in (UserStatus.ACTIVE, UserStatus.INACTIVE):
            raise ValueError("status는 ACTIVE 또는 INACTIVE만 허용됩니다")
        return v


class AdminUserResponse(BaseModel):
    """어드민 계정 목록 항목"""

    model_config = ConfigDict(populate_by_name=True)

    uid: int
    email: str
    name: str
    status: UserStatus = Field(description="계정 상태 (ACTIVE 등)")
    role: AdminUserRole = Field(description="어드민 권한 (ADMIN/OWNER — OWNER는 삭제 불가)")
    joined_date: datetime = Field(serialization_alias="joinedDate")


class AdminLoginRequest(BaseModel):
    email: EmailStr
    password: str = Field(min_length=1)


class AdminLoginResponse(BaseModel):
    model_config = ConfigDict(populate_by_name=True)

    uid: int
    access_token: str = Field(alias="accessToken", serialization_alias="accessToken")
    email: str
    name: str
    role: AdminUserRole = Field(description="어드민 권한 (ADMIN/OWNER)")


class AdminValidateTokenResponse(BaseModel):
    uid: int
    email: str
    name: str
    role: AdminUserRole = Field(description="어드민 권한 (ADMIN/OWNER)")


class AdminAuthData(BaseModel):
    uid: int
    access_token: str
