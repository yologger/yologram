package link.yologram.api.v1.domain.search.tech.service

import io.github.oshai.kotlinlogging.KotlinLogging
import link.yologram.api.v1.domain.pms.tech.repository.TechPostRepository
import link.yologram.api.v1.domain.search.exception.InvalidIndexRangeException
import link.yologram.api.v1.domain.search.tech.publisher.message.TechIndexingMessage
import link.yologram.api.v1.domain.search.tech.publisher.message.TechIndexingMessagePublisher
import org.springframework.scheduling.annotation.Async
import org.springframework.stereotype.Service

/**
 * 게시글 인덱싱 요청 — 실제 인덱싱은 하지 않고 SQS에 작업만 넣는다.
 *
 * 레거시(yologram-legacy BoardIndexingService)는 워커가 없어 발행과 소비를 한 애플리케이션에서 했지만,
 * 우리는 worker가 소비하므로 api는 발행까지만 담당한다 — OpenSearch 클라이언트가 필요 없다.
 *
 * 큰 범위를 그대로 한 메시지에 담지 않고 CHUNK_SIZE로 쪼개는 이유:
 *   ① 한 메시지의 처리 시간이 SQS 가시성 타임아웃(300초)을 넘으면 그 메시지가 재노출돼 중복 처리된다
 *   ② 실패 시 재시도 단위가 작아진다(10만 건 한 덩어리가 아니라 20건)
 *   ③ 워커를 늘리면 메시지 단위로 병렬 처리된다
 *
 * 이 클래스는 pms의 TechPostRepository를 직접 참조한다 — 같은 애플리케이션 안의 읽기 전용 조회(max id)이고,
 * 인덱싱은 pms 데이터를 검색용으로 복제하는 작업이라 경계를 넘는 것이 본질이다.
 */
@Service
class AdminTechPostIndexingService(
    private val postRepository: TechPostRepository,
    private val publisher: TechIndexingMessagePublisher,
) {

    private val logger = KotlinLogging.logger {}

    /** 단건 — from == to로 보내 범위 인덱싱과 같은 경로를 탄다 */
    fun index(id: Long) {
        publisher.publish(TechIndexingMessage(from = id, to = id))
    }

    /** 범위 — CHUNK_SIZE 단위로 쪼개 발행. 반환값은 발행한 메시지 수 */
    fun index(from: Long, to: Long): Int {
        // 도메인 예외로 던져야 400이 된다 — IllegalArgumentException은 전역 폴백에서 500이 된다
        if (from > to || from < 1) throw InvalidIndexRangeException()

        var current = from
        var published = 0
        while (current <= to) {
            val chunkTo = minOf(current + CHUNK_SIZE - 1, to)
            publisher.publish(TechIndexingMessage(from = current, to = chunkTo))
            published++
            current = chunkTo + 1
        }
        logger.info { "post index requested: range=$from-$to messages=$published" }
        return published
    }

    /**
     * 전체(비동기) — 어드민 요청은 이 메서드로 받는다. 발행 루프를 요청 스레드에서 돌리지 않는다:
     * 게시글이 10만 건이면 SendMessage를 5,000번 호출하는 동안 응답을 못 주고 게이트웨이 타임아웃(30초)에 걸린다.
     *
     * @Async 메서드의 예외는 호출자에게 전달되지 않으므로 여기서 직접 잡아 남긴다
     * (Spring 기본 핸들러에 맡기면 어느 범위까지 발행됐는지 알 수 없다).
     * 진행 상황은 예외가 아니라 SQS 큐 깊이로 확인한다.
     *
     * 범위·단건은 동기 그대로 둔다 — 범위는 어드민이 크기를 정하고 단건은 메시지 하나라 길어질 일이 없다.
     */
    @Async("sqsTaskExecutor")
    fun fullIndexAsync() {
        runCatching { fullIndex() }
            .onFailure { logger.error(it) { "full post index publish failed" } }
    }

    /**
     * 전체 — max id까지 훑는다. 삭제된 id 구간은 워커가 조회 결과 0건으로 흘려보낸다(무해).
     * 반환값은 발행한 메시지 수, 글이 없으면 0.
     */
    fun fullIndex(): Int {
        val maxId = postRepository.findMaxId()
        if (maxId == null || maxId <= 0) {
            logger.info { "no posts to index" }
            return 0
        }
        logger.info { "full post index requested: maxId=$maxId" }
        return index(from = 1, to = maxId)
    }

    companion object {
        /** 메시지 1건이 담는 id 범위 크기 (레거시 BULK_INDEXING_REQUEST_BATCH_SIZE=20 미러) */
        const val CHUNK_SIZE = 20L
    }
}
