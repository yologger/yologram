package link.yologram.api.v1.infra.cache

import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.module.kotlin.jacksonTypeRef
import java.time.Duration

/**
 * 캐시 항목 정의 — 키·값 타입·TTL을 한 곳에 묶는다 (레거시 yologram-api infra/cache 미러).
 * 키 스킴: {도메인 prefix}:v1:{엔티티}:{식별자}
 */
data class Cache<V>(
    val key: String,
    val type: TypeReference<V>,
    val duration: Duration,
) {
    companion object Factory {

        private const val USER_PREFIX = "ums:users"

        /**
         * 유저 닉네임 캐시.
         * 닉네임 변경·탈퇴 시 명시적 무효화(deleteAll)가 주 수단이고, TTL 1시간은 무효화 누락 대비 보험.
         */
        fun userNickname(uid: Long) = Cache<String>(
            key = "$USER_PREFIX:v1:nickname:$uid",
            type = jacksonTypeRef(),
            duration = Duration.ofHours(1),
        )
    }
}
