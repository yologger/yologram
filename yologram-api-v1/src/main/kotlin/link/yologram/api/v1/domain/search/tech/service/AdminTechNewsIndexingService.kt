package link.yologram.api.v1.domain.search.tech.service

import io.github.oshai.kotlinlogging.KotlinLogging
import link.yologram.api.v1.domain.news.tech.repository.TechNewsRepository
import link.yologram.api.v1.domain.search.tech.publisher.message.TechIndexingMessage
import org.springframework.scheduling.annotation.Async
import org.springframework.stereotype.Service

/**
 * 뉴스 인덱싱 요청 — 게시글과 같은 구조이고 대상(target)만 다르다.
 *
 * 평상시 뉴스 색인은 worker가 요약 직후 직접 한다(실시간). 이 API는 그 경로가 놓친 구간을 메우는 용도다 —
 * 색인 실패로 빠진 건, 매핑 변경 후 재색인, 검색을 나중에 켠 경우의 과거 데이터.
 */
@Service
class AdminTechNewsIndexingService(
    private val newsRepository: TechNewsRepository,
    private val indexingPublisher: AdminTechIndexingPublisher,
) {

    private val logger = KotlinLogging.logger {}

    fun index(id: Long) = indexingPublisher.publishSingle(TechIndexingMessage.TARGET_TECH_NEWS, id)

    fun index(from: Long, to: Long): Int =
        indexingPublisher.publishRange(TechIndexingMessage.TARGET_TECH_NEWS, from, to)

    /** 전체(비동기) — 게시글과 같은 이유로 @Async (발행 루프가 길어 요청 스레드를 잡으면 타임아웃) */
    @Async("sqsTaskExecutor")
    fun fullIndexAsync() {
        runCatching { fullIndex() }
            .onFailure { logger.error(it) { "full news index publish failed" } }
    }

    fun fullIndex(): Int =
        indexingPublisher.publishFull(TechIndexingMessage.TARGET_TECH_NEWS, newsRepository.findMaxId())
}
