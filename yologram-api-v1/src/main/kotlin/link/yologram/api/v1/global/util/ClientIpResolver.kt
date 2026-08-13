package link.yologram.api.v1.global.util

import jakarta.servlet.http.HttpServletRequest

/**
 * 클라이언트 IP 추출. 우선순위는 세 단계다.
 *
 * ① X-Client-Ip — API Gateway(HTTP API)가 $context.identity.sourceIp를 넣어주는 커스텀 헤더.
 *    HTTP API + private integration(VPC Link)에서는 remoteAddr이 VPC 내부 주소이고
 *    X-Forwarded-For는 파라미터 매핑 예약 헤더라 채울 수 없어, 원 클라이언트 IP를 얻는 유일한 경로다.
 *    게이트웨이가 overwrite로 넣으므로 클라이언트가 위조해 보내도 덮인다 (yologram-infra 통합 설정).
 * ② X-Forwarded-For 첫 값 — CloudFront·ALB 등 XFF를 채우는 경로용 폴백.
 * ③ remoteAddr — 프록시가 없는 로컬 개발용.
 */
object ClientIpResolver {

    private const val HEADER_X_CLIENT_IP = "X-Client-Ip"
    private const val HEADER_X_FORWARDED_FOR = "X-Forwarded-For"

    fun resolve(request: HttpServletRequest): String? =
        request.getHeader(HEADER_X_CLIENT_IP)
            ?.trim()
            ?.takeIf { it.isNotBlank() }
        // 프록시 체인이면 "client, proxy1, proxy2" 형태 — 맨 앞이 원 클라이언트
            ?: request.getHeader(HEADER_X_FORWARDED_FOR)
                ?.substringBefore(',')
                ?.trim()
                ?.takeIf { it.isNotBlank() }
            ?: request.remoteAddr
}
