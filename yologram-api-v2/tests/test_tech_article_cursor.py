import base64
from datetime import datetime

import pytest

from app.core.exception import InvalidCursorException
from app.domain.tech.article.cursor import TechArticleCursor


def _b64(raw: str) -> str:
    return base64.urlsafe_b64encode(raw.encode()).decode().rstrip("=")


class TestTechArticleCursor:

    def test_인코딩한_커서를_디코딩하면_원래_값이_나온다(self):
        published_at = datetime(2026, 7, 18, 9, 30, 15)

        encoded = TechArticleCursor.encode(published_at, 123)
        decoded = TechArticleCursor.decode(encoded)

        assert decoded.published_at == published_at
        assert decoded.id == 123

    def test_초가_0인_발행_시각도_왕복된다(self):
        published_at = datetime(2026, 7, 18, 9, 0, 0)

        decoded = TechArticleCursor.decode(TechArticleCursor.encode(published_at, 1))

        assert decoded.published_at == published_at

    def test_api_v1과_동일_인코딩__초가_0이면_초_생략(self):
        # Java ISO_LOCAL_DATE_TIME은 초가 0이면 "…T09:00"으로 초를 생략 — 바이트 동일해야 상호 호환
        encoded = TechArticleCursor.encode(datetime(2026, 7, 18, 9, 0, 0), 1)

        assert encoded == _b64("2026-07-18T09:00|1")

    def test_api_v1과_동일_인코딩__초가_있으면_초_포함(self):
        encoded = TechArticleCursor.encode(datetime(2026, 7, 18, 9, 30, 15), 123)

        assert encoded == _b64("2026-07-18T09:30:15|123")

    def test_api_v1이_인코딩한_커서를_디코딩한다(self):
        # api-v1(Java)이 만든 초 생략 형식 토큰
        decoded = TechArticleCursor.decode(_b64("2026-07-18T09:00|42"))

        assert decoded.published_at == datetime(2026, 7, 18, 9, 0, 0)
        assert decoded.id == 42

    def test_base64가_아닌_값이면_예외가_발생한다(self):
        with pytest.raises(InvalidCursorException):
            TechArticleCursor.decode("@@@잘못된값@@@")

    def test_형식이_다른_base64면_예외가_발생한다(self):
        with pytest.raises(InvalidCursorException):
            TechArticleCursor.decode(_b64("no-delimiter"))

    def test_id가_숫자가_아니면_예외가_발생한다(self):
        with pytest.raises(InvalidCursorException):
            TechArticleCursor.decode(_b64("2026-07-18T09:00:00|abc"))
