package link.yologram.api.v1.domain.ums.service

import link.yologram.api.v1.domain.ums.entity.User
import link.yologram.api.v1.domain.ums.exception.AuthWrongPasswordException
import link.yologram.api.v1.domain.ums.exception.UserDuplicateException
import link.yologram.api.v1.domain.ums.exception.UserNotFoundException
import link.yologram.api.v1.domain.ums.model.ChangePasswordRequest
import link.yologram.api.v1.domain.ums.model.JoinRequest
import link.yologram.api.v1.domain.ums.model.JoinResponse
import link.yologram.api.v1.domain.ums.model.UpdateProfileRequest
import link.yologram.api.v1.domain.ums.model.UserMeResponse
import link.yologram.api.v1.domain.ums.repository.UserRepository
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class UserService(
    private val userRepository: UserRepository,
    private val passwordEncoder: BCryptPasswordEncoder,
) {

    @Transactional
    fun join(request: JoinRequest): JoinResponse {
        if (userRepository.existsByEmail(request.email)) {
            throw UserDuplicateException()
        }

        val user = User(
            email = request.email,
            name = request.name,
            nickname = request.nickname,
            password = passwordEncoder.encode(request.password),
        )

        val saved = userRepository.save(user)
        return JoinResponse(uid = saved.id)
    }

    @Transactional(readOnly = true)
    fun getMe(uid: Long): UserMeResponse {
        val user = userRepository.findById(uid)
            .orElseThrow { UserNotFoundException() }

        return UserMeResponse(
            uid = user.id,
            email = user.email,
            name = user.name,
            nickname = user.nickname,
            avatar = user.avatar,
            type = user.type,
            joinedDate = user.joinedDate,
        )
    }

    @Transactional
    fun updateProfile(uid: Long, request: UpdateProfileRequest): UserMeResponse {
        val user = userRepository.findById(uid)
            .orElseThrow { UserNotFoundException() }

        user.nickname = request.nickname

        return UserMeResponse(
            uid = user.id,
            email = user.email,
            name = user.name,
            nickname = user.nickname,
            avatar = user.avatar,
            type = user.type,
            joinedDate = user.joinedDate,
        )
    }

    @Transactional
    fun changePassword(uid: Long, request: ChangePasswordRequest) {
        val user = userRepository.findById(uid)
            .orElseThrow { UserNotFoundException() }

        if (!passwordEncoder.matches(request.currentPassword, user.password)) {
            throw AuthWrongPasswordException()
        }

        user.password = passwordEncoder.encode(request.newPassword)
    }
}
