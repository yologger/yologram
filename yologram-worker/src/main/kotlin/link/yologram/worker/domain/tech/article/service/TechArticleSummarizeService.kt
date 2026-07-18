package link.yologram.worker.domain.tech.article.service

import io.github.oshai.kotlinlogging.KotlinLogging
import link.yologram.worker.domain.tech.article.client.ArticleContentCrawler
import link.yologram.worker.domain.tech.article.entity.TechArticle
import link.yologram.worker.domain.tech.article.enums.TechArticleStatus
import link.yologram.worker.domain.tech.article.repository.TechArticleRepository
import link.yologram.worker.global.discord.DiscordNotifier
import link.yologram.worker.global.llm.LlmClient
import org.springframework.beans.factory.ObjectProvider
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Sort
import org.springframework.stereotype.Service

private val logger = KotlinLogging.logger {}

@Service
class TechArticleSummarizeService(
    private val techArticleRepository: TechArticleRepository,
    private val articleContentCrawler: ArticleContentCrawler,
    private val llmClient: LlmClient,
    // Discord 알림은 yologram.discord.enabled=true 일 때만 빈 존재 — 없으면 스킵
    private val discordNotifier: ObjectProvider<DiscordNotifier>,
) {

    /**
     * COLLECTED 상태 아티클을 배치로 요약: 원문 크롤링 → LLM(Gemini→Groq fallback) → SUMMARIZED.
     * 실패 시 retry_count 증가, 한도(MAX_RETRY) 도달 시 FAILED(터미널).
     * status가 작업 큐 역할 — RSS 재노출과 무관하게 DB 기준으로 재시도 (멱등).
     */
    fun summarize(): SummarizeResult {
        if (!llmClient.available) {
            logger.warn { "LLM 제공자 미구성 — 요약 스킵 (yologram.llm.*.api-key 확인)" }
            return SummarizeResult(targetCount = 0, summarizedCount = 0, failedCount = 0)
        }

        val targets = techArticleRepository.findByStatusAndRetryCountLessThan(
            TechArticleStatus.COLLECTED,
            MAX_RETRY,
            PageRequest.of(0, BATCH_SIZE, Sort.by(Sort.Direction.ASC, "id")),
        )
        if (targets.isEmpty()) return SummarizeResult(targetCount = 0, summarizedCount = 0, failedCount = 0)

        var summarized = 0
        var failed = 0

        for (article in targets) {
            runCatching {
                val content = articleContentCrawler.fetch(article.link)
                llmClient.complete(buildPrompt(article.title, article.link, content))
            }.onSuccess { completion ->
                article.summary = completion.content
                article.status = TechArticleStatus.SUMMARIZED
                summarized++
                logger.info { "테크 아티클 요약 완료: id=${article.id} provider=${completion.provider}" }
                notifyDiscord(article)
            }.onFailure { e ->
                failed++
                article.retryCount += 1
                if (article.retryCount >= MAX_RETRY) {
                    article.status = TechArticleStatus.FAILED
                    notifyFailed(article, e)
                }
                logger.error(e) {
                    "테크 아티클 요약 실패: id=${article.id} link=${article.link} " +
                        "retryCount=${article.retryCount} status=${article.status}"
                }
            }
            techArticleRepository.save(article)
        }

        logger.info { "테크 아티클 요약 배치 완료: targets=${targets.size} summarized=$summarized failed=$failed" }
        return SummarizeResult(targetCount = targets.size, summarizedCount = summarized, failedCount = failed)
    }

    /**
     * 재시도 소진으로 FAILED 확정된 순간 개발자 경고 — FAILED는 터미널이라 자가 회복이 멈추는 유일한 지점.
     * (재시도 중 실패는 다음 주기가 커버하므로 알리지 않음)
     */
    private fun notifyFailed(article: TechArticle, cause: Throwable) {
        discordNotifier.ifAvailable {
            it.send(
                DiscordNotifier.CHANNEL_TECH,
                "⚠️ 요약 최종 실패(FAILED): [${article.sourceName}] ${article.title}\n" +
                    "<${article.link}>\n" +
                    "사유: ${cause.message}"
            )
        }
    }

    /** 요약이 완성된 글만 Discord embed로 발송 (n8n 알림 대체). 발송 실패는 DiscordNotifier가 삼킴 */
    private fun notifyDiscord(article: TechArticle) {
        discordNotifier.ifAvailable {
            it.sendEmbed(
                channel = DiscordNotifier.CHANNEL_TECH,
                title = article.title,
                url = article.link,
                description = article.summary.orEmpty(),
                sourceName = article.sourceName,
            )
        }
    }

    private fun buildPrompt(title: String, link: String, content: String) = """
        나는 DevOps, Backend 엔지니어고
        당신은 RSS 피드 기사를 독자에게 전달할 요약으로 정리하는 전문 에디터입니다.
        아래 기사를 읽고 핵심만 간결하게 정리해주세요.

        # 출력 형식 (반드시 이 구조를 따를 것)

        **📌 한 줄 요약**
        (기사의 핵심을 25자 이내로)

        **🔑 핵심 포인트**
        - (육하원칙 기반 사실 1, 한 문장)
        - (육하원칙 기반 사실 2, 한 문장)
        - (육하원칙 기반 사실 3, 한 문장)
        - (육하원칙 기반 사실 4, 한 문장)
        - (육하원칙 기반 사실 5, 한 문장)
        - (육하원칙 기반 사실 6, 한 문장)
        - (더 전달해야할 핵심 포인트가 있으면 더 추가 가능)

        **💡 왜 중요한가**
        (이 소식이 독자에게 주는 시사점을 1~2문장으로)

        **📖 핵심 개념 & 용어**
        - 글에서 가장 중요한 핵심개념 2~4개 정도로 설명.

        # 작성 규칙
        - 전체 길이는 300자 이내로 제한
        - 추측이나 원문에 없는 정보는 절대 추가하지 말 것
        - 불필요한 서론("요약해 드리겠습니다" 등)은 생략하고 본론만 출력
        - 마케팅성 수식어(혁신적인, 놀라운 등)는 제거
        - 불확실한 내용은 "~로 알려졌다", "~할 전망" 등으로 표현
        - 한국어로 작성, 전문 용어는 영문 병기 (예: 쿠버네티스(Kubernetes))
        - ~입니다, ~에요, ~이다 같은 어미를 절대 붙이지 말고, 문장을 간결하게 마침표(.)로 표현

        # 입력
        제목: $title
        링크: $link
        내용: $content
    """.trimIndent()

    data class SummarizeResult(
        val targetCount: Int,
        val summarizedCount: Int,
        val failedCount: Int,
    )

    companion object {
        // 배치당 처리 건수 — Gemini 무료 티어 10 RPM 이내
        const val BATCH_SIZE = 10

        // 재시도 한도 — 도달 시 FAILED (5분 주기 × 5회 = 약 25분치 일시 장애 흡수, 초과는 영구 실패 간주)
        const val MAX_RETRY = 5
    }
}
