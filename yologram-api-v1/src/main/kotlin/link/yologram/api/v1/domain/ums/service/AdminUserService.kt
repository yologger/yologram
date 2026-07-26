package link.yologram.api.v1.domain.ums.service

import link.yologram.api.v1.domain.ums.entity.AdminUser
import link.yologram.api.v1.domain.ums.exception.AdminUserDuplicateException
import link.yologram.api.v1.domain.ums.exception.AdminUserNotFoundException
import link.yologram.api.v1.domain.ums.exception.AuthWrongPasswordException
import link.yologram.api.v1.domain.ums.model.AdminLoginRequest
import link.yologram.api.v1.domain.ums.model.AdminLoginResponse
import link.yologram.api.v1.domain.ums.model.AdminUserCreateRequest
import link.yologram.api.v1.domain.ums.model.AdminUserCreateResponse
import link.yologram.api.v1.domain.ums.model.AdminValidateTokenResponse
import link.yologram.api.v1.domain.ums.repository.AdminUserRepository
import link.yologram.api.v1.domain.ums.util.AdminJwtUtil
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class AdminUserService(
    private val adminUserRepository: AdminUserRepository,
    private val passwordEncoder: BCryptPasswordEncoder,
    private val adminJwtUtil: AdminJwtUtil,
) {

    @Transactional
    fun create(request: AdminUserCreateRequest): AdminUserCreateResponse {
        if (adminUserRepository.existsByEmail(request.email)) {
            throw AdminUserDuplicateException()
        }

        val admin = AdminUser(
            email = request.email,
            name = request.name,
            password = passwordEncoder.encode(request.password),
        )

        val saved = adminUserRepository.save(admin)
        return AdminUserCreateResponse(uid = saved.id)
    }

    fun login(request: AdminLoginRequest): AdminLoginResponse {
        val admin = adminUserRepository.findByEmail(request.email)
            .orElseThrow { AdminUserNotFoundException() }

        if (!passwordEncoder.matches(request.password, admin.password)) {
            throw AuthWrongPasswordException()
        }

        val accessToken = adminJwtUtil.createToken(admin.id)

        return AdminLoginResponse(
            uid = admin.id,
            accessToken = accessToken,
            email = admin.email,
            name = admin.name,
        )
    }

    @Transactional(readOnly = true)
    fun validateToken(token: String): AdminValidateTokenResponse {
        val uid = adminJwtUtil.validateAndGetUid(token)
        val admin = adminUserRepository.findById(uid)
            .orElseThrow { AdminUserNotFoundException() }

        return AdminValidateTokenResponse(
            uid = admin.id,
            email = admin.email,
            name = admin.name,
        )
    }

    fun logout(uid: Long) {
    }
}
