from datetime import datetime

from pydantic import BaseModel, ConfigDict, EmailStr, Field


class JoinRequest(BaseModel):
    email: EmailStr
    name: str = Field(min_length=2, max_length=20)
    nickname: str = Field(min_length=2, max_length=20)
    password: str = Field(min_length=8, max_length=20)


class JoinResponse(BaseModel):
    uid: int


class UserMeResponse(BaseModel):
    model_config = ConfigDict(populate_by_name=True, from_attributes=True)

    uid: int
    email: str
    name: str
    nickname: str
    avatar: str | None
    type: str
    joined_date: datetime = Field(serialization_alias="joinedDate")
