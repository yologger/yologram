from fastapi import Request

HEADER_X_FORWARDED_FOR = "X-Forwarded-For"


def resolve_client_ip(request: Request) -> str | None:
    """
    클라이언트 IP 추출 (api-v1 ClientIpResolver 미러). API Gateway·CloudFront를 거치면
    접속 주소(request.client.host)는 프록시 IP라 X-Forwarded-For의 첫 값(원 클라이언트)을 우선한다.
    헤더가 없거나 비어 있으면 접속 주소로 폴백 (TestClient 등 클라이언트 정보가 없으면 None).
    """
    # 프록시 체인이면 "client, proxy1, proxy2" 형태 — 맨 앞이 원 클라이언트
    forwarded_for = request.headers.get(HEADER_X_FORWARDED_FOR)
    if forwarded_for:
        client_ip = forwarded_for.split(",")[0].strip()
        if client_ip:
            return client_ip

    return request.client.host if request.client else None
