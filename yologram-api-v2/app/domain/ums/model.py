from datetime import datetime

from sqlalchemy import BigInteger, Boolean, Column, DateTime, Enum, String, func

from app.config.database import Base
from app.domain.ums.enum import AdminUserRole, UserStatus, UserType


class User(Base):
    __tablename__ = "user"

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


class AdminUser(Base):
    __tablename__ = "admin_user"

    id = Column(BigInteger, primary_key=True, autoincrement=True)
    email = Column(String(200), nullable=False, unique=True)
    name = Column(String(200), nullable=False)
    password = Column(String(200), nullable=False)
    status = Column(Enum(UserStatus), nullable=False, default=UserStatus.ACTIVE)
    # OWNER는 DB 직접 조작 전용 — API 생성은 항상 ADMIN
    role = Column(Enum(AdminUserRole), nullable=False, default=AdminUserRole.ADMIN)
    joined_date = Column(DateTime, nullable=False, default=func.now())
    modified_date = Column(DateTime, nullable=False, default=func.now(), onupdate=func.now())


class UserEmailVerification(Base):
    __tablename__ = "user_email_verification"

    id = Column(BigInteger, primary_key=True, autoincrement=True)
    email = Column(String(200), nullable=False)
    code = Column(String(6), nullable=False)
    verified = Column(Boolean, nullable=False, default=False)
    expired_at = Column(DateTime, nullable=False)
    created_at = Column(DateTime, nullable=False, default=func.now())


class UserPasswordResetCode(Base):
    __tablename__ = "user_password_reset_code"

    id = Column(BigInteger, primary_key=True, autoincrement=True)
    email = Column(String(200), nullable=False)
    code = Column(String(6), nullable=False)
    verified = Column(Boolean, nullable=False, default=False)
    expired_at = Column(DateTime, nullable=False)
    created_at = Column(DateTime, nullable=False, default=func.now())
