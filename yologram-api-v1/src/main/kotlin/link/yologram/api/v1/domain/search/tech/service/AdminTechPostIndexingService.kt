package link.yologram.api.v1.domain.search.tech.service

import io.github.oshai.kotlinlogging.KotlinLogging
import link.yologram.api.v1.domain.pms.tech.repository.TechPostRepository
import link.yologram.api.v1.domain.search.tech.publisher.message.TechIndexingMessage
import org.springframework.scheduling.annotation.Async
import org.springframework.stereotype.Service

/**
 * 게시글 인덱싱 요청 — 쪼개기·발행은 AdminTechIndexingPublisher가 하고 여기서는 대상과 범위만 정한다.
 *
 * pms의 TechPostRepository를 직접 참조한다 — 같은 애플리케이션 안의 읽기 전용 조회(max id)이고,
 * 인덱싱은 pms 데이터를 검색용으로 복제하는 작업이라 경계를 넘는 것이 본질이다.
 */
@Service
class AdminTechPostIndexingService(
    private val postRepository: TechPostRepository,
    private val indexingPublisher: AdminTechIndexingPublisher,
) {

    private val logger = KotlinLogging.logger {}

    fun index(id: Long) = indexingPublisher.publishSingle(TechIndexingMessage.TARGET_TECH_POST, id)

    fun index(from: Long, to: Long): Int =
        indexingPublisher.publishRange(TechIndexingMessage.TARGET_TECH_POST, from, to)

    /**
     * 전체(비동기) — 어드민 요청은 이 메서드로 받는다. 발행 루프를 요청 스레드에서 돌리지 않는다:
     * 게시글이 10만 건이면 SendMessage를 5,000번 호출하는 동안 응답을 못 주고 게이트웨이 타임아웃(30초)에 걸린다.
     *
     * @Async 메서드의 예외는 호출자에게 전달되지 않으므로 여기서 직접 잡아 남긴다.
     * 진행 상황은 예외가 아니라 SQS 큐 깊이로 확인한다.
     */
    @Async("sqsTaskExecutor")
    fun fullIndexAsync() {
        runCatching { fullIndex() }
            .onFailure { logger.error(it) { "full post index publish failed" } }
    }

    fun fullIndex(): Int =
        indexingPublisher.publishFull(TechIndexingMessage.TARGET_TECH_POST, postRepository.findMaxId())
}
