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
