import base64

from app.core.exception import InvalidCursorException


class PostCursor:
    """
    게시글 피드 cursor (keyset 페이지네이션).
    id desc 정렬 기준(id가 작성순=시간순). 마지막 글 id를 Base64(URL-safe)로 인코딩.
    """

    @staticmethod
    def encode(post_id: int) -> str:
        return base64.urlsafe_b64encode(str(post_id).encode()).decode().rstrip("=")

    @staticmethod
    def decode(value: str) -> int:
        try:
            padded = value + "=" * (-len(value) % 4)
            return int(base64.urlsafe_b64decode(padded).decode())
        except Exception as e:
            raise InvalidCursorException() from e
