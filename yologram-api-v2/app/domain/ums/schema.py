from pydantic import BaseModel, EmailStr, Field


class JoinRequest(BaseModel):
    email: EmailStr
    name: str = Field(min_length=2, max_length=20)
    nickname: str = Field(min_length=2, max_length=20)
    password: str = Field(min_length=8, max_length=20)


class JoinResponse(BaseModel):
    uid: int
