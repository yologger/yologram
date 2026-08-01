package link.yologram.worker.domain.tech.news.scheduler

import link.yologram.worker.domain.tech.news.service.TechNewsSummarizeService
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component

@Component
class TechNewsSummarizeScheduler(
    private val techNewsSummarizeService: TechNewsSummarizeService,
) {
    @Scheduled(cron = "\${yologram.tech-news.summarize.cron:0 0/5 * * * *}")
    fun summarize() {
        techNewsSummarizeService.summarize()
    }
}
