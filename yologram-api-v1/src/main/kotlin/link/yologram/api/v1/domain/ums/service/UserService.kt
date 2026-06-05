package link.yologram.api.v1.domain.ums.service

import link.yologram.api.v1.domain.ums.entity.User
import link.yologram.api.v1.domain.ums.exception.UserDuplicateException
import link.yologram.api.v1.domain.ums.model.JoinRequest
import link.yologram.api.v1.domain.ums.model.JoinResponse
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
}
