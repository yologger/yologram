package link.yologram.worker.domain.news.tech.scheduler

import link.yologram.worker.domain.news.tech.service.TechNewsSummarizeService
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component

@Component
class TechNewsSummarizeScheduler(
    private val techNewsSummarizeService: TechNewsSummarizeService,
) {
    @Scheduled(cron = "\${yologram.batches.tech-news-summarize.schedule:0 0/5 * * * *}")
    fun summarize() {
        techNewsSummarizeService.summarize()
    }
}
