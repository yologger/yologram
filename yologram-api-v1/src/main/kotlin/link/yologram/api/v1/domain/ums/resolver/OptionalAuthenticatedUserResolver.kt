package link.yologram.api.v1.domain.ums.resolver

import link.yologram.api.v1.domain.ums.exception.AuthTokenInvalidException
import link.yologram.api.v1.domain.ums.util.JwtUtil
import org.springframework.core.MethodParameter
import org.springframework.stereotype.Component
import org.springframework.web.bind.support.WebDataBinderFactory
import org.springframework.web.context.request.NativeWebRequest
import org.springframework.web.method.support.HandlerMethodArgumentResolver
import org.springframework.web.method.support.ModelAndViewContainer

/**
 * @OptionalAuthenticatedUser + AuthData? 파라미터 처리 (AuthenticatedUserResolver의 선택 인증판).
 * 헤더 부재만 null로 허용하고, 헤더가 있으면 필수 인증과 동일하게 검증한다 (무효 토큰 401).
 */
@Component
class OptionalAuthenticatedUserResolver(
    private val jwtUtil: JwtUtil,
) : HandlerMethodArgumentResolver {

    override fun supportsParameter(parameter: MethodParameter): Boolean {
        return parameter.hasParameterAnnotation(OptionalAuthenticatedUser::class.java)
                && parameter.parameterType == AuthData::class.java
    }

    override fun resolveArgument(
        parameter: MethodParameter,
        mavContainer: ModelAndViewContainer?,
        webRequest: NativeWebRequest,
        binderFactory: WebDataBinderFactory?,
    ): AuthData? {
        // 헤더 자체가 없으면 비로그인 — null 주입 (공개 API 유지)
        val authHeader = webRequest.getHeader("Authorization") ?: return null

        // 헤더를 보냈다면 필수 인증과 동일 규칙 — 형식 불량·무효 토큰은 401
        if (!authHeader.startsWith("Bearer ") || authHeader.length <= 7) {
            throw AuthTokenInvalidException()
        }

        val token = authHeader.substring(7).trim()
        if (token.isEmpty()) {
            throw AuthTokenInvalidException()
        }
        val uid = jwtUtil.validateAndGetUid(token)

        return AuthData(uid = uid, accessToken = token)
    }
}
