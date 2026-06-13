from pydantic import BaseModel, ConfigDict, EmailStr, Field


class LoginRequest(BaseModel):
    email: EmailStr
    password: str = Field(min_length=1)


class LoginResponse(BaseModel):
    model_config = ConfigDict(populate_by_name=True)

    uid: int
    access_token: str = Field(alias="accessToken", serialization_alias="accessToken")
    email: str
    name: str
    nickname: str


class ValidateTokenResponse(BaseModel):
    uid: int
    email: str
    name: str
    nickname: str


class AuthData(BaseModel):
    uid: int
    access_token: str


class EmailVerificationSendRequest(BaseModel):
    email: EmailStr


class EmailVerificationVerifyRequest(BaseModel):
    email: EmailStr
    code: str = Field(min_length=6, max_length=6)


class PasswordResetSendRequest(BaseModel):
    email: EmailStr


class PasswordResetVerifyRequest(BaseModel):
    email: EmailStr
    code: str = Field(min_length=6, max_length=6)


class PasswordResetConfirmRequest(BaseModel):
    model_config = ConfigDict(populate_by_name=True)

    email: EmailStr
    code: str = Field(min_length=6, max_length=6)
    new_password: str = Field(min_length=8, max_length=20, alias="newPassword")
