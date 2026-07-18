package link.yologram.worker.domain.tech.article.scheduler

import link.yologram.worker.domain.tech.article.service.TechArticleSummarizeService
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component

@Component
class TechArticleSummarizeScheduler(
    private val techArticleSummarizeService: TechArticleSummarizeService,
) {
    @Scheduled(cron = "\${yologram.tech-article.summarize.cron:0 0/5 * * * *}")
    fun summarize() {
        techArticleSummarizeService.summarize()
    }
}
