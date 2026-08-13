from starlette.requests import Request

from app.core.client_ip import resolve_client_ip


def _request(headers: dict[str, str] | None = None, client: tuple[str, int] | None = ("10.0.0.1", 1234)) -> Request:
    """ASGI scope로 만든 요청 모형 — 헤더·접속 주소 조합만 바꿔 IP 추출을 검증."""
    scope: dict = {
        "type": "http",
        "method": "GET",
        "path": "/api/v2/pms/tech/posts/1",
        "headers": [(k.lower().encode(), v.encode()) for k, v in (headers or {}).items()],
    }
    if client is not None:
        scope["client"] = client
    return Request(scope)


class TestResolveClientIp:

    def test_X_Client_Ip가_있으면_그_값(self):
        assert resolve_client_ip(_request({"X-Client-Ip": "1.2.3.4"})) == "1.2.3.4"

    def test_X_Client_Ip는_X_Forwarded_For보다_우선한다(self):
        # 게이트웨이가 overwrite로 넣는 값이라 클라이언트가 보낼 수 있는 XFF보다 신뢰도가 높다
        request = _request({"X-Client-Ip": "1.2.3.4", "X-Forwarded-For": "9.9.9.9"})

        assert resolve_client_ip(request) == "1.2.3.4"

    def test_X_Client_Ip가_공백만이면_X_Forwarded_For로_폴백(self):
        request = _request({"X-Client-Ip": "   ", "X-Forwarded-For": "9.9.9.9"})

        assert resolve_client_ip(request) == "9.9.9.9"

    def test_X_Client_Ip_앞뒤_공백은_제거한다(self):
        assert resolve_client_ip(_request({"X-Client-Ip": "  1.2.3.4  "})) == "1.2.3.4"

    def test_X_Client_Ip_헤더_이름_대소문자는_무관하다(self):
        assert resolve_client_ip(_request({"x-client-ip": "1.2.3.4"})) == "1.2.3.4"

    def test_두_헤더_모두_없으면_접속_주소로_폴백(self):
        assert resolve_client_ip(_request()) == "10.0.0.1"

    def test_X_Forwarded_For_단일_값이면_그_값(self):
        assert resolve_client_ip(_request({"X-Forwarded-For": "1.2.3.4"})) == "1.2.3.4"

    def test_프록시_체인이면_첫_값이_원_클라이언트(self):
        request = _request({"X-Forwarded-For": "1.2.3.4, 70.41.3.18, 150.172.238.178"})

        assert resolve_client_ip(request) == "1.2.3.4"

    def test_첫_값_앞뒤_공백은_제거한다(self):
        assert resolve_client_ip(_request({"X-Forwarded-For": "  1.2.3.4 , 5.6.7.8"})) == "1.2.3.4"

    def test_헤더가_없으면_접속_주소로_폴백(self):
        assert resolve_client_ip(_request()) == "10.0.0.1"

    def test_헤더가_빈_문자열이면_접속_주소로_폴백(self):
        assert resolve_client_ip(_request({"X-Forwarded-For": ""})) == "10.0.0.1"

    def test_헤더가_공백만이면_접속_주소로_폴백(self):
        assert resolve_client_ip(_request({"X-Forwarded-For": "   "})) == "10.0.0.1"

    def test_헤더_이름_대소문자는_무관하다(self):
        assert resolve_client_ip(_request({"x-forwarded-for": "1.2.3.4"})) == "1.2.3.4"

    def test_헤더도_접속_주소도_없으면_None(self):
        assert resolve_client_ip(_request(client=None)) is None
