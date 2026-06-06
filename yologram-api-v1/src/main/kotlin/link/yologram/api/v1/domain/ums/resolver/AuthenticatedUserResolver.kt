package link.yologram.api.v1.domain.ums.resolver

import link.yologram.api.v1.domain.ums.exception.AuthTokenInvalidException
import link.yologram.api.v1.domain.ums.util.JwtUtil
import org.springframework.core.MethodParameter
import org.springframework.stereotype.Component
import org.springframework.web.bind.support.WebDataBinderFactory
import org.springframework.web.context.request.NativeWebRequest
import org.springframework.web.method.support.HandlerMethodArgumentResolver
import org.springframework.web.method.support.ModelAndViewContainer

@Component
class AuthenticatedUserResolver(
    private val jwtUtil: JwtUtil,
) : HandlerMethodArgumentResolver {

    override fun supportsParameter(parameter: MethodParameter): Boolean {
        return parameter.hasParameterAnnotation(AuthenticatedUser::class.java)
                && parameter.parameterType == AuthData::class.java
    }

    override fun resolveArgument(
        parameter: MethodParameter,
        mavContainer: ModelAndViewContainer?,
        webRequest: NativeWebRequest,
        binderFactory: WebDataBinderFactory?,
    ): AuthData {
        val authHeader = webRequest.getHeader("Authorization")
            ?: throw AuthTokenInvalidException()

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
