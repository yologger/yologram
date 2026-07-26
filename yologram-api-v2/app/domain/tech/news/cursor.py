import base64
from dataclasses import dataclass
from datetime import datetime

from app.core.exception import InvalidCursorException

_DELIMITER = "|"


@dataclass(frozen=True)
class TechArticleCursor:
    """
    테크 아티클 피드 cursor (keyset 페이지네이션).
    정렬 기준이 published_at desc라 유일하지 않음 — (publishedAt, id) 복합 커서로
    동일 발행 시각의 페이지 경계 누락·중복을 방지 (id가 tie-breaker).
    "ISO발행시각|id"를 Base64(URL-safe)로 인코딩 — api-v1 TechArticleCursor와 동일 인코딩(상호 호환).
    """

    published_at: datetime
    id: int

    @staticmethod
    def encode(published_at: datetime, article_id: int) -> str:
        raw = f"{_format_iso_local_date_time(published_at)}{_DELIMITER}{article_id}"
        return base64.urlsafe_b64encode(raw.encode()).decode().rstrip("=")

    @staticmethod
    def decode(value: str) -> "TechArticleCursor":
        try:
            padded = value + "=" * (-len(value) % 4)
            raw = base64.urlsafe_b64decode(padded).decode()
            published_at_raw, id_raw = raw.split(_DELIMITER, 1)
            return TechArticleCursor(published_at=datetime.fromisoformat(published_at_raw), id=int(id_raw))
        except InvalidCursorException:
            raise
        except Exception as e:
            raise InvalidCursorException() from e


def _format_iso_local_date_time(value: datetime) -> str:
    """
    Java DateTimeFormatter.ISO_LOCAL_DATE_TIME과 바이트 동일 포맷 —
    초·소수부가 0이면 초 생략, 소수부는 3자리 그룹 최소 표기 (api-v1 커서와 상호 호환 필수).
    """
    text = value.strftime("%Y-%m-%dT%H:%M")
    if value.second == 0 and value.microsecond == 0:
        return text
    text += f":{value.second:02d}"
    if value.microsecond:
        fraction = f"{value.microsecond:06d}"
        if fraction.endswith("000"):
            fraction = fraction[:3]
        text += f".{fraction}"
    return text
