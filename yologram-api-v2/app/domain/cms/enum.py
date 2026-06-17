import enum

from app.core.exception import InvalidSectionException


class Section(str, enum.Enum):
    TECH = "TECH"
    INVEST = "INVEST"
    POLITICS = "POLITICS"

    @classmethod
    def from_path(cls, path: str) -> "Section":
        for section in cls:
            if section.value.lower() == path.lower():
                return section
        raise InvalidSectionException()
