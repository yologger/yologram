package link.yologram.api.v1.domain.search.tech.service

import io.github.oshai.kotlinlogging.KotlinLogging
import link.yologram.api.v1.domain.search.exception.InvalidIndexRangeException
import link.yologram.api.v1.domain.search.tech.publisher.message.TechIndexingMessage
import link.yologram.api.v1.domain.search.tech.publisher.message.TechIndexingMessagePublisher
import org.springframework.stereotype.Component

/**
 * 인덱싱 작업 발행 공통 로직 — 대상(게시글·뉴스)이 달라도 쪼개는 방식은 같다.
 *
 * 큰 범위를 그대로 한 메시지에 담지 않고 CHUNK_SIZE로 쪼개는 이유:
 *   ① 한 메시지의 처리 시간이 SQS 가시성 타임아웃(300초)을 넘으면 그 메시지가 재노출돼 중복 처리된다
 *   ② 실패 시 재시도 단위가 작아진다(10만 건 한 덩어리가 아니라 20건)
 *   ③ 워커를 늘리면 메시지 단위로 병렬 처리된다
 *
 * 실제 인덱싱은 하지 않는다 — worker가 소비하므로 api는 발행까지만 담당한다
 * (레거시 BoardIndexingService는 워커가 없어 한 애플리케이션에서 발행·소비를 모두 했다).
 */
@Component
class AdminTechIndexingPublisher(
    private val publisher: TechIndexingMessagePublisher,
) {

    private val logger = KotlinLogging.logger {}

    /** 범위를 CHUNK_SIZE 단위로 쪼개 발행. 반환값은 발행한 메시지 수 */
    fun publishRange(target: String, from: Long, to: Long): Int {
        // 도메인 예외로 던져야 400이 된다 — IllegalArgumentException은 전역 폴백에서 500이 된다
        if (from > to || from < 1) throw InvalidIndexRangeException()

        var current = from
        var published = 0
        while (current <= to) {
            val chunkTo = minOf(current + CHUNK_SIZE - 1, to)
            publisher.publish(TechIndexingMessage(target = target, from = current, to = chunkTo))
            published++
            current = chunkTo + 1
        }
        logger.info { "index requested: target=$target range=$from-$to messages=$published" }
        return published
    }

    /** 단건 — from == to로 보내 범위 인덱싱과 같은 경로를 탄다 */
    fun publishSingle(target: String, id: Long) {
        publisher.publish(TechIndexingMessage(target = target, from = id, to = id))
    }

    /**
     * 전체 — max id까지 훑는다. 삭제된 id 구간은 워커가 조회 결과 0건으로 흘려보낸다(무해).
     * maxId가 없거나 0 이하면 발행하지 않는다.
     */
    fun publishFull(target: String, maxId: Long?): Int {
        if (maxId == null || maxId <= 0) {
            logger.info { "no documents to index: target=$target" }
            return 0
        }
        logger.info { "full index requested: target=$target maxId=$maxId" }
        return publishRange(target, from = 1, to = maxId)
    }

    companion object {
        /** 메시지 1건이 담는 id 범위 크기 (레거시 BULK_INDEXING_REQUEST_BATCH_SIZE=20 미러) */
        const val CHUNK_SIZE = 20L
    }
}
