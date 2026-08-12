package link.yologram.api.v1.global.util

import jakarta.servlet.http.HttpServletRequest

/**
 * 클라이언트 IP 추출 — API Gateway·CloudFront를 거치면 remoteAddr은 프록시 IP라
 * X-Forwarded-For의 첫 값(원 클라이언트)을 우선한다. 헤더가 없으면 remoteAddr로 폴백.
 */
object ClientIpResolver {

    private const val HEADER_X_FORWARDED_FOR = "X-Forwarded-For"

    fun resolve(request: HttpServletRequest): String? =
        // 프록시 체인이면 "client, proxy1, proxy2" 형태 — 맨 앞이 원 클라이언트
        request.getHeader(HEADER_X_FORWARDED_FOR)
            ?.substringBefore(',')
            ?.trim()
            ?.takeIf { it.isNotBlank() }
            ?: request.remoteAddr
}
