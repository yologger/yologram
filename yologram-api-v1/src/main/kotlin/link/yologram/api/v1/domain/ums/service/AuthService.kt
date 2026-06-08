package link.yologram.api.v1.domain.ums.service

import link.yologram.api.v1.domain.ums.exception.AuthWrongPasswordException
import link.yologram.api.v1.domain.ums.exception.UserNotFoundException
import link.yologram.api.v1.domain.ums.model.LoginRequest
import link.yologram.api.v1.domain.ums.model.LoginResponse
import link.yologram.api.v1.domain.ums.model.ValidateTokenResponse
import link.yologram.api.v1.domain.ums.repository.UserRepository
import link.yologram.api.v1.domain.ums.util.JwtUtil
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class AuthService(
    private val userRepository: UserRepository,
    private val passwordEncoder: BCryptPasswordEncoder,
    private val jwtUtil: JwtUtil,
) {

    fun login(request: LoginRequest): LoginResponse {
        val user = userRepository.findByEmail(request.email)
            .orElseThrow { UserNotFoundException() }

        if (!passwordEncoder.matches(request.password, user.password)) {
            throw AuthWrongPasswordException()
        }

        val accessToken = jwtUtil.createToken(user.id)

        return LoginResponse(
            uid = user.id,
            accessToken = accessToken,
            email = user.email,
            name = user.name,
            nickname = user.nickname,
        )
    }

    @Transactional(readOnly = true)
    fun validateToken(token: String): ValidateTokenResponse {
        val uid = jwtUtil.validateAndGetUid(token)
        val user = userRepository.findById(uid)
            .orElseThrow { UserNotFoundException() }

        return ValidateTokenResponse(
            uid = user.id,
            email = user.email,
            name = user.name,
            nickname = user.nickname,
        )
    }

    fun logout(uid: Long) {
    }
}
