package link.yologram.worker.domain.news.tech.scheduler

import link.yologram.worker.domain.news.tech.service.TechNewsCollectService
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component

@Component
class TechNewsCollectScheduler(
    private val techNewsCollectService: TechNewsCollectService,
) {

    /**
     * 주기 수집 (10분마다, Asia/Seoul) — 놓친 회차는 다음 회차가 커버 (Spot 중단 허용, 멱등).
     * 테스트에서는 cron "-"(CRON_DISABLED)로 비활성화.
     */
    @Scheduled(cron = "\${yologram.tech-news.collect.cron:0 0/10 * * * *}")
    fun collect() {
        techNewsCollectService.collect()
    }
}
