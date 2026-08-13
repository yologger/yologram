from fastapi import Request

HEADER_X_CLIENT_IP = "X-Client-Ip"
HEADER_X_FORWARDED_FOR = "X-Forwarded-For"


def resolve_client_ip(request: Request) -> str | None:
    """
    클라이언트 IP 추출 (api-v1 ClientIpResolver 미러). 우선순위는 아래 세 단계다.

    ① X-Client-Ip — API Gateway(HTTP API)가 $context.identity.sourceIp를 넣어주는 커스텀 헤더.
       HTTP API + private integration(VPC Link)에서는 접속 주소가 VPC 내부 주소이고
       X-Forwarded-For는 파라미터 매핑 예약 헤더라 채울 수 없어, 원 클라이언트 IP를 얻는 유일한 경로다.
       게이트웨이가 overwrite로 넣으므로 클라이언트가 위조해 보내도 덮인다 (yologram-infra 통합 설정).
    ② X-Forwarded-For 첫 값 — CloudFront·ALB 등 XFF를 채우는 경로용 폴백.
    ③ 접속 주소(request.client.host) — 프록시가 없는 로컬 개발용. TestClient면 None일 수 있다.
    """
    client_ip = request.headers.get(HEADER_X_CLIENT_IP)
    if client_ip and client_ip.strip():
        return client_ip.strip()

    # 프록시 체인이면 "client, proxy1, proxy2" 형태 — 맨 앞이 원 클라이언트
    forwarded_for = request.headers.get(HEADER_X_FORWARDED_FOR)
    if forwarded_for:
        client_ip = forwarded_for.split(",")[0].strip()
        if client_ip:
            return client_ip

    return request.client.host if request.client else None
