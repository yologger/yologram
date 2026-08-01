import enum


class UserType(str, enum.Enum):
    DEFAULT = "DEFAULT"
    POLITICIAN = "POLITICIAN"
    ECONOMIST = "ECONOMIST"
    ADMIN = "ADMIN"


class UserStatus(str, enum.Enum):
    ACTIVE = "ACTIVE"
    INACTIVE = "INACTIVE"
    DELETED = "DELETED"


class AdminUserRole(str, enum.Enum):
    """어드민 권한. OWNER는 삭제 불가한 최상위 계정 — 부여는 DB 직접 조작 전용 (API로 생성 불가)"""

    ADMIN = "ADMIN"
    OWNER = "OWNER"
