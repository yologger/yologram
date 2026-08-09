package link.yologram.api.v1.infra.cache

import link.yologram.api.v1.domain.news.tech.model.TechNewsResponse
import link.yologram.api.v1.global.model.ApiEnvelopCursorPage
import org.springframework.stereotype.Component

/**
 * 테크 뉴스 첫 페이지 cache-aside 공용 컴포넌트.
 *
 * - 조회는 페이지 키 GET 1회 — 버전 키 간접층 없음.
 * - 무효화: worker가 새 뉴스 요약(SUMMARIZED) 시 첫 페이지 키를 전수 열거해 UNLINK —
 *   다음 요청부터 미스가 나 최신 목록이 다시 캐시된다 (worker TechNewsFirstPageCacheInvalidator와 키 계약).
 * - TTL 3분은 보험: 삭제 실패(Redis 순단·worker 다운)나 레이스(삭제 직후, 커밋 전에 읽은
 *   옛 목록이 SET으로 부활)로 낡은 캐시가 남아도 3분이 생존 상한.
 * - 첫 페이지(cursor == null)만 대상 — 커서 페이지는 키가 커서 값으로 분산돼 히트율이 없어 제외.
 * - Redis 장애 시 CacheService가 실패를 삼켜 미스와 동일 경로 → loader(DB) 폴백, 기능 무손상.
 *
 * UserNicknameCache와 동일하게 DB 조회는 loader 람다로 주입받아
 * 도메인 경계(리포지토리·클라이언트 소유)는 도메인 서비스에 남긴다.
 */
@Component
class TechNewsFirstPageCache(
    private val cacheService: CacheService,
) {

    /** cache-aside: 페이지 키 히트 시 반환, 미스 시 loader → 결과 캐시 후 반환. */
    fun get(
        categoryId: Long?,
        size: Int,
        loader: () -> ApiEnvelopCursorPage<TechNewsResponse>,
    ): ApiEnvelopCursorPage<TechNewsResponse> {
        val cache = Cache.techNewsFirstPage(categoryId, size)
        cacheService.getOrNull(cache)?.let { return it }

        val loaded = loader()
        cacheService.set(cache, loaded)
        return loaded
    }
}
