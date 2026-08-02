package link.yologram.api.v1.domain.ums.service

import link.yologram.api.v1.domain.ums.entity.User
import link.yologram.api.v1.domain.ums.exception.AuthWrongPasswordException
import link.yologram.api.v1.domain.ums.exception.UserEmailNotVerifiedException
import link.yologram.api.v1.domain.ums.exception.UserDuplicateException
import link.yologram.api.v1.domain.ums.exception.UserNotFoundException
import link.yologram.api.v1.domain.ums.model.ChangePasswordRequest
import link.yologram.api.v1.domain.ums.model.JoinRequest
import link.yologram.api.v1.domain.ums.model.JoinResponse
import link.yologram.api.v1.domain.ums.model.UpdateProfileRequest
import link.yologram.api.v1.domain.ums.model.UserMeResponse
import link.yologram.api.v1.domain.ums.repository.UserEmailVerificationRepository
import link.yologram.api.v1.domain.ums.repository.UserRepository
import link.yologram.api.v1.infra.cache.Cache
import link.yologram.api.v1.infra.cache.CacheService
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class UserService(
    private val userRepository: UserRepository,
    private val emailVerificationCodeRepository: UserEmailVerificationRepository,
    private val passwordEncoder: BCryptPasswordEncoder,
    private val cacheService: CacheService,
) {

    @Transactional
    fun join(request: JoinRequest): JoinResponse {
        if (userRepository.existsByEmail(request.email)) {
            throw UserDuplicateException()
        }

        val verification = emailVerificationCodeRepository.findTopByEmailOrderByCreatedAtDesc(request.email)
        if (verification.isEmpty || !verification.get().verified) {
            throw UserEmailNotVerifiedException()
        }

        val user = User(
            email = request.email,
            name = request.name,
            nickname = request.nickname,
            password = passwordEncoder.encode(request.password),
        )

        val saved = userRepository.save(user)
        emailVerificationCodeRepository.deleteAllByEmail(request.email)
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

        // 닉네임 캐시 무효화 — 커밋 후(afterCommit) 실행이 이상적이나 레거시 스타일의 단순 호출 유지.
        // 커밋 전 삭제 후 다른 요청이 옛 값을 재적재하는 짧은 레이스는 TTL 1시간 보험으로 수렴
        cacheService.deleteAll(Cache.userNickname(uid))

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

    @Transactional
    fun withdraw(uid: Long) {
        // 개발 단계: 탈퇴 시 레코드를 하드 삭제 (email 즉시 해제 → 재가입 가능)
        // 추후: soft delete(status=DELETED) + 유예 후 정리/익명화 + 탈퇴 유저 차단으로 전환
        val user = userRepository.findById(uid)
            .orElseThrow { UserNotFoundException() }

        userRepository.delete(user)

        // 닉네임 캐시 무효화 (updateProfile과 동일한 단순 호출 방식 — 근거는 updateProfile 주석 참조)
        cacheService.deleteAll(Cache.userNickname(uid))
    }
}
