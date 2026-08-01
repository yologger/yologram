from enum import Enum


class CommentSort(str, Enum):
    """
    댓글 정렬 기준. LATEST(최신순, id desc, 기본) / OLDEST(오래된순, id asc).
    쿼리 파라미터는 관대하게 해석: "oldest"만 OLDEST, 그 외(미지정·오타 포함)는 LATEST 기본.
    """

    LATEST = "LATEST"
    OLDEST = "OLDEST"

    @classmethod
    def from_param(cls, value: str | None) -> "CommentSort":
        if value is not None and value.lower() == "oldest":
            return cls.OLDEST
        return cls.LATEST
