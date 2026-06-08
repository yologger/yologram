from datetime import datetime

from sqlalchemy import BigInteger, Column, DateTime, Enum, String, func

from app.config.database import Base
from app.domain.ums.enum import UserStatus, UserType


class User(Base):
    __tablename__ = "users"

    id = Column(BigInteger, primary_key=True, autoincrement=True)
    email = Column(String(200), nullable=False, unique=True)
    name = Column(String(200), nullable=False)
    nickname = Column(String(200), nullable=False)
    password = Column(String(200), nullable=False)
    avatar = Column(String(512), nullable=True)
    type = Column(Enum(UserType), nullable=False, default=UserType.DEFAULT)
    status = Column(Enum(UserStatus), nullable=False, default=UserStatus.ACTIVE)
    deleted_date = Column(DateTime, nullable=True)
    joined_date = Column(DateTime, nullable=False, default=func.now())
    modified_date = Column(DateTime, nullable=False, default=func.now(), onupdate=func.now())
