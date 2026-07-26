package link.yologram.api.v1.domain.ums.resolver

import link.yologram.api.v1.domain.ums.exception.AuthTokenInvalidException
import link.yologram.api.v1.domain.ums.util.AdminJwtUtil
import org.springframework.core.MethodParameter
import org.springframework.stereotype.Component
import org.springframework.web.bind.support.WebDataBinderFactory
import org.springframework.web.context.request.NativeWebRequest
import org.springframework.web.method.support.HandlerMethodArgumentResolver
import org.springframework.web.method.support.ModelAndViewContainer

@Component
class AuthenticatedAdminUserResolver(
    private val adminJwtUtil: AdminJwtUtil,
) : HandlerMethodArgumentResolver {

    override fun supportsParameter(parameter: MethodParameter): Boolean {
        return parameter.hasParameterAnnotation(AuthenticatedAdminUser::class.java)
                && parameter.parameterType == AdminAuthData::class.java
    }

    override fun resolveArgument(
        parameter: MethodParameter,
        mavContainer: ModelAndViewContainer?,
        webRequest: NativeWebRequest,
        binderFactory: WebDataBinderFactory?,
    ): AdminAuthData {
        val authHeader = webRequest.getHeader("Authorization")
            ?: throw AuthTokenInvalidException()

        if (!authHeader.startsWith("Bearer ") || authHeader.length <= 7) {
            throw AuthTokenInvalidException()
        }

        val token = authHeader.substring(7).trim()
        if (token.isEmpty()) {
            throw AuthTokenInvalidException()
        }
        val uid = adminJwtUtil.validateAndGetUid(token)

        return AdminAuthData(uid = uid, accessToken = token)
    }
}
