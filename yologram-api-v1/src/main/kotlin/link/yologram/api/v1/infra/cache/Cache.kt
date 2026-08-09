package link.yologram.api.v1.infra.cache

import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.module.kotlin.jacksonTypeRef
import link.yologram.api.v1.domain.news.tech.model.TechNewsResponse
import link.yologram.api.v1.global.model.ApiEnvelopCursorPage
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

        private const val NEWS_PREFIX = "news:tech"

        /**
         * 테크 뉴스 첫 페이지 캐시 (cursor 없는 요청 전용 — 트래픽 대부분이 첫 페이지).
         * 무효화는 worker가 요약 완료 시 키 전수 열거 UNLINK — 키 스킴·size 상한(1~50)은
         * worker TechNewsFirstPageCacheInvalidator와의 문자열 계약. TTL 3분은 삭제 누락·
         * 레이스(삭제 직후 옛 목록 SET 부활) 대비 보험이자 낡음의 상한.
         * categoryId 미지정은 "all", size는 정규화(1~MAX) 후 전달해 키 폭증을 막는다.
         */
        fun techNewsFirstPage(categoryId: Long?, size: Int) = Cache<ApiEnvelopCursorPage<TechNewsResponse>>(
            key = "$NEWS_PREFIX:v1:first-page:${categoryId ?: "all"}:$size",
            type = jacksonTypeRef(),
            duration = Duration.ofMinutes(3),
        )
    }
}
