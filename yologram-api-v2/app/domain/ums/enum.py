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
