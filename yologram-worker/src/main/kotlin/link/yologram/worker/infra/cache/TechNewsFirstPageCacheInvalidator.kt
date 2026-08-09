package link.yologram.worker.infra.cache

import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.data.redis.core.StringRedisTemplate
import org.springframework.stereotype.Component

private val logger = KotlinLogging.logger {}

/**
 * 테크 뉴스 첫 페이지 캐시 무효화 — 키 전수 열거 UNLINK.
 *
 * API(v1·v2)는 테크 뉴스 첫 페이지를 `news:tech:v1:first-page:{categoryId|all}:{size}` 키로
 * 캐시한다(버전 키 없음 — 조회는 GET 1회). 워커가 새 뉴스를 SUMMARIZED로 전환하면 이 클래스가
 * 해당 키들을 지워 다음 요청부터 최신 목록이 다시 캐시되게 한다.
 *
 * SCAN 대신 키 전수 열거인 이유: SCAN은 prefix 인덱스가 아니라 전체 keyspace 순회라
 * 닉네임 캐시 등 다른 키가 늘수록 비용이 같이 자란다. 우리 키 공간은
 * (all + 활성 카테고리) × size(1~MAX_PAGE_SIZE)로 열거 가능하고, UNLINK는 없는 키를
 * O(1)로 무시하므로 존재 여부와 무관하게 정확한 키 목록을 한 번의 명령으로 지울 수 있다.
 * (UNLINK는 DEL의 비동기판 — 실제 해제를 백그라운드로 미뤄 메인 스레드를 막지 않음)
 *
 * 삭제 실패(Redis 다운 등)는 warn 로그만 남기고 삼킨다 — 무효화는 부가 기능이라 요약 배치의
 * 성공에 영향을 주면 안 되고, 이 경우 API 캐시 TTL 3분이 보험으로 동작한다.
 *
 * 알려진 레이스: API가 DB에서 옛 목록을 읽는 도중(커밋 전) 삭제가 끼어들면 직후 SET으로
 * 낡은 캐시가 되살아날 수 있음 — 생존 상한이 TTL 3분이고 발생 창이 수십 ms라 수용 (done.md).
 */
@Component
class TechNewsFirstPageCacheInvalidator(
    private val stringRedisTemplate: StringRedisTemplate,
) {

    /**
     * 테크 뉴스 첫 페이지 캐시 전체 삭제 — 요약 배치에서 SUMMARIZED 전환이 1건 이상일 때 배치당 1회 호출.
     * @param categoryIds 활성 카테고리 전체(tech_category 마스터) — all 스코프와 함께 키 열거에 사용
     */
    fun clear(categoryIds: Collection<Long>) {
        val scopes = listOf(ALL_SCOPE) + categoryIds.toSortedSet().map { it.toString() }
        val keys = scopes.flatMap { scope ->
            (1..MAX_PAGE_SIZE).map { size -> pageKey(scope, size) }
        }
        runCatching {
                stringRedisTemplate.unlink(keys) // DEL <key>
            }
            .onSuccess { removed ->
                logger.info { "테크 뉴스 첫 페이지 캐시 삭제: 열거 ${keys.size}개 중 ${removed}개 제거" }
            }
            .onFailure { e ->
                logger.warn(e) { "테크 뉴스 첫 페이지 캐시 삭제 실패 — API 캐시 TTL 3분이 보험 (열거 ${keys.size}개)" }
            }
    }

    companion object {
        /**
         * API와의 문자열 계약 — api-v1 infra/cache/Cache.kt·api-v2 cache.py의
         * 첫 페이지 캐시 키(`news:tech:v1:first-page:{categoryId|all}:{size}`)와 동일해야 한다.
         */
        private const val KEY_PREFIX = "news:tech:v1:first-page"

        /** 카테고리 필터 없는 전체 피드 스코프 */
        const val ALL_SCOPE = "all"

        /** API 첫 페이지 size 상한 계약 (api-v1 TechNewsService.MAX_PAGE_SIZE와 동일 값) */
        const val MAX_PAGE_SIZE = 50

        fun pageKey(scope: String, size: Int) = "$KEY_PREFIX:$scope:$size"
    }
}
