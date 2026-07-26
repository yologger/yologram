package link.yologram.api.v1.domain.ums.util

import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import com.auth0.jwt.exceptions.JWTVerificationException
import com.auth0.jwt.exceptions.TokenExpiredException
import link.yologram.api.v1.config.AdminJwtProperties
import link.yologram.api.v1.domain.ums.exception.AuthTokenExpiredException
import link.yologram.api.v1.domain.ums.exception.AuthTokenInvalidException
import org.springframework.stereotype.Component
import java.util.*

@Component
class AdminJwtUtil(
    private val adminJwtProperties: AdminJwtProperties,
) {

    private val algorithm: Algorithm = Algorithm.HMAC256(adminJwtProperties.secret)

    fun createToken(uid: Long): String {
        return JWT.create()
            .withIssuer(adminJwtProperties.issuer)
            .withAudience(adminJwtProperties.audience)
            .withClaim("uid", uid)
            .withExpiresAt(Date(System.currentTimeMillis() + adminJwtProperties.expire * 1000))
            .sign(algorithm)
    }

    fun validateAndGetUid(token: String): Long {
        try {
            val verifier = JWT.require(algorithm)
                .withIssuer(adminJwtProperties.issuer)
                .withAudience(adminJwtProperties.audience)
                .build()
            val decoded = verifier.verify(token)
            return decoded.getClaim("uid").asLong()
        } catch (e: TokenExpiredException) {
            throw AuthTokenExpiredException()
        } catch (e: JWTVerificationException) {
            throw AuthTokenInvalidException()
        }
    }
}
