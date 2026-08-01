from datetime import datetime

from pydantic import BaseModel, ConfigDict, EmailStr, Field

from app.domain.ums.enum import UserStatus


class AdminUserCreateRequest(BaseModel):
    email: EmailStr
    name: str = Field(min_length=2, max_length=20)
    password: str = Field(min_length=8, max_length=20)


class AdminUserCreateResponse(BaseModel):
    uid: int


class AdminUserResponse(BaseModel):
    """어드민 계정 목록 항목"""

    model_config = ConfigDict(populate_by_name=True)

    uid: int
    email: str
    name: str
    status: UserStatus = Field(description="계정 상태 (ACTIVE 등)")
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


class AdminValidateTokenResponse(BaseModel):
    uid: int
    email: str
    name: str


class AdminAuthData(BaseModel):
    uid: int
    access_token: str
