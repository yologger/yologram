package link.yologram.api.v1.domain.ums.service

import link.yologram.api.v1.domain.ums.entity.AdminUser
import link.yologram.api.v1.domain.ums.enum.AdminUserRole
import link.yologram.api.v1.domain.ums.enum.UserStatus
import link.yologram.api.v1.domain.ums.exception.AdminRoleForbiddenException
import link.yologram.api.v1.domain.ums.exception.AdminUserDuplicateException
import link.yologram.api.v1.domain.ums.exception.AdminUserInactiveException
import link.yologram.api.v1.domain.ums.exception.AdminUserNotFoundException
import link.yologram.api.v1.domain.ums.exception.AdminUserOwnerImmutableException
import link.yologram.api.v1.domain.ums.exception.AdminUserOwnerUndeletableException
import link.yologram.api.v1.domain.ums.exception.AdminUserSelfDeleteException
import link.yologram.api.v1.domain.ums.exception.AuthWrongPasswordException
import link.yologram.api.v1.domain.ums.model.AdminLoginRequest
import link.yologram.api.v1.domain.ums.model.AdminLoginResponse
import link.yologram.api.v1.domain.ums.model.AdminUserCreateRequest
import link.yologram.api.v1.domain.ums.model.AdminUserCreateResponse
import link.yologram.api.v1.domain.ums.model.AdminUserResponse
import link.yologram.api.v1.domain.ums.model.AdminValidateTokenResponse
import link.yologram.api.v1.domain.ums.repository.AdminUserRepository
import link.yologram.api.v1.domain.ums.util.AdminJwtUtil
import link.yologram.api.v1.global.model.ApiEnvelopPage
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Sort
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

        // role은 요청으로 받지 않고 항상 ADMIN — OWNER는 DB 직접 조작으로만 존재·변경 (정책)
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

        // 비밀번호 검증 후 상태 체크 — 계정 존재·상태 정보 노출 최소화
        if (admin.status == UserStatus.INACTIVE) {
            throw AdminUserInactiveException()
        }

        val accessToken = adminJwtUtil.createToken(admin.id)

        return AdminLoginResponse(
            uid = admin.id,
            accessToken = accessToken,
            email = admin.email,
            name = admin.name,
            role = admin.role,
        )
    }

    @Transactional(readOnly = true)
    fun validateToken(token: String): AdminValidateTokenResponse {
        val uid = adminJwtUtil.validateAndGetUid(token)
        val admin = adminUserRepository.findById(uid)
            .orElseThrow { AdminUserNotFoundException() }

        if (admin.status == UserStatus.INACTIVE) {
            throw AdminUserInactiveException()
        }

        return AdminValidateTokenResponse(
            uid = admin.id,
            email = admin.email,
            name = admin.name,
            role = admin.role,
        )
    }

    fun logout(uid: Long) {
    }

    /** 어드민 목록 offset 페이지 조회 (id asc) — 단순 페이지 조회라 QueryDSL 없이 Spring Data Pageable 사용 */
    @Transactional(readOnly = true)
    fun getAdminUsers(page: Int, size: Int): ApiEnvelopPage<AdminUserResponse> {
        val result = adminUserRepository.findAll(PageRequest.of(page, size, Sort.by("id").ascending()))
        return ApiEnvelopPage(
            data = result.content.map { AdminUserResponse.from(it) },
            page = result.number.toLong(),
            size = result.size.toLong(),
            totalPages = result.totalPages.toLong(),
            totalCount = result.totalElements,
            first = result.isFirst,
            last = result.isLast,
        )
    }

    /**
     * hard delete — 자기 자신 삭제 금지 규칙만으로 어드민이 항상 최소 1명 남는 것이 보장됨.
     * 검사 순서: 자기 자신(400) → 존재 여부(404) → OWNER 보호(400).
     */
    @Transactional
    fun delete(requesterUid: Long, id: Long) {
        if (requesterUid == id) {
            throw AdminUserSelfDeleteException()
        }
        val admin = adminUserRepository.findById(id)
            .orElseThrow { AdminUserNotFoundException() }
        if (admin.role == AdminUserRole.OWNER) {
            throw AdminUserOwnerUndeletableException()
        }
        adminUserRepository.delete(admin)
    }

    /**
     * 어드민 활성/비활성 (OWNER 전용).
     * 검사 순서: 요청자 role(403) → 대상 존재(404) → 대상 OWNER 보호(400).
     * 요청자가 OWNER뿐이고 대상 OWNER는 차단되므로 자기 자신 변경도 자동 차단됨.
     */
    @Transactional
    fun updateStatus(requesterUid: Long, id: Long, status: UserStatus): AdminUserResponse {
        val requester = adminUserRepository.findById(requesterUid)
            .orElseThrow { AdminUserNotFoundException() }
        if (requester.role != AdminUserRole.OWNER) {
            throw AdminRoleForbiddenException()
        }

        val target = adminUserRepository.findById(id)
            .orElseThrow { AdminUserNotFoundException() }
        if (target.role == AdminUserRole.OWNER) {
            throw AdminUserOwnerImmutableException()
        }

        target.status = status
        // @LastModifiedDate는 flush 시점에 갱신되므로 응답에 반영되도록 즉시 flush
        val saved = adminUserRepository.saveAndFlush(target)
        return AdminUserResponse.from(saved)
    }
}
