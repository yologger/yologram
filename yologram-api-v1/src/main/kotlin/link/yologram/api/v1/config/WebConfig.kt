package link.yologram.api.v1.config

import link.yologram.api.v1.domain.ums.resolver.AuthenticatedAdminUserResolver
import link.yologram.api.v1.domain.ums.resolver.AuthenticatedUserResolver
import link.yologram.api.v1.domain.ums.resolver.OptionalAuthenticatedUserResolver
import org.springframework.context.annotation.Configuration
import org.springframework.web.method.support.HandlerMethodArgumentResolver
import org.springframework.web.servlet.config.annotation.CorsRegistry
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer

@Configuration
class WebConfig(
    private val authenticatedUserResolver: AuthenticatedUserResolver,
    private val authenticatedAdminResolver: AuthenticatedAdminUserResolver,
    private val optionalAuthenticatedUserResolver: OptionalAuthenticatedUserResolver,
) : WebMvcConfigurer {

    override fun addCorsMappings(registry: CorsRegistry) {
        registry.addMapping("/**")
            .allowedOrigins("*")
            .allowedMethods("*")
            .allowedHeaders("*")
    }

    override fun addArgumentResolvers(resolvers: MutableList<HandlerMethodArgumentResolver>) {
        resolvers.add(authenticatedUserResolver)
        resolvers.add(authenticatedAdminResolver)
        // 선택 인증(공개 API + 로그인 시 likedByMe 등 개인화) — 헤더 없으면 null 주입
        resolvers.add(optionalAuthenticatedUserResolver)
    }
}
