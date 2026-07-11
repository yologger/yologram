package link.yologram.worker.domain.test.scheduler

import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component

private val logger = KotlinLogging.logger {}

/**
 * 배포 검증용 테스트 스케줄러 — 1분마다 로그 출력.
 * 워커 스케줄 동작(기동·Spot 재기동 후 재개) 확인 후 실제 잡(RSS 수집 등) 도입 시 제거.
 */
@Component
class TestScheduler {

    @Scheduled(fixedRate = 60_000)
    fun logTest() {
        logger.info { "테스트" }
    }
}
