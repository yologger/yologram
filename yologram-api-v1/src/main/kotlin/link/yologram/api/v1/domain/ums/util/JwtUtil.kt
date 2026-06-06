package link.yologram.api.v1.domain.ums.util

import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import com.auth0.jwt.exceptions.JWTVerificationException
import com.auth0.jwt.exceptions.TokenExpiredException
import link.yologram.api.v1.config.JwtProperties
import link.yologram.api.v1.domain.ums.exception.AuthTokenExpiredException
import link.yologram.api.v1.domain.ums.exception.AuthTokenInvalidException
import org.springframework.stereotype.Component
import java.util.*

@Component
class JwtUtil(
    private val jwtProperties: JwtProperties,
) {

    private val algorithm: Algorithm = Algorithm.HMAC256(jwtProperties.secret)

    fun createToken(uid: Long): String {
        return JWT.create()
            .withIssuer(jwtProperties.issuer)
            .withAudience(jwtProperties.audience)
            .withClaim("uid", uid)
            .withExpiresAt(Date(System.currentTimeMillis() + jwtProperties.expire * 1000))
            .sign(algorithm)
    }

    fun validateAndGetUid(token: String): Long {
        try {
            val verifier = JWT.require(algorithm)
                .withIssuer(jwtProperties.issuer)
                .withAudience(jwtProperties.audience)
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
