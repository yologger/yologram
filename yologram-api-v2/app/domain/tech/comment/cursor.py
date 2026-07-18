import base64

from app.core.exception import InvalidCursorException


class TechPostCommentCursor:
    """
    테크 댓글 목록 cursor (keyset 페이지네이션).
    정렬 기준 id(작성순=시간순)의 마지막 값을 Base64(URL-safe)로 인코딩.
    최신순은 id < cursor, 오래된순은 id > cursor로 이어받는다.
    """

    @staticmethod
    def encode(comment_id: int) -> str:
        return base64.urlsafe_b64encode(str(comment_id).encode()).decode().rstrip("=")

    @staticmethod
    def decode(value: str) -> int:
        try:
            padded = value + "=" * (-len(value) % 4)
            return int(base64.urlsafe_b64decode(padded).decode())
        except Exception as e:
            raise InvalidCursorException() from e
