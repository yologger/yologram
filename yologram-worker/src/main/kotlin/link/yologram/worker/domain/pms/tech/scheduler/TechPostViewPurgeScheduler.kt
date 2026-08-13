package link.yologram.worker.domain.pms.tech.scheduler

import link.yologram.worker.domain.pms.tech.service.TechPostViewPurgeService
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component

@Component
class TechPostViewPurgeScheduler(
    private val techPostViewPurgeService: TechPostViewPurgeService,
) {

    /**
     * 조회 이력 보관 기간 정리 (하루 1회 04:30, Asia/Seoul — 트래픽이 가장 적은 시간대).
     * 놓친 회차는 다음 회차가 커버 (Spot 중단 허용 — 임계 시각 기준 삭제라 소급 불필요·멱등).
     * 테스트에서는 schedule "-"(CRON_DISABLED)로 비활성화.
     */
    @Scheduled(cron = "\${yologram.batches.post-view-purge.schedule:0 30 4 * * *}")
    fun purge() {
        techPostViewPurgeService.purge()
    }
}
