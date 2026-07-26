package link.yologram.worker.domain.tech.article.service

import io.github.oshai.kotlinlogging.KotlinLogging
import link.yologram.worker.domain.tech.article.client.RssFeedClient
import link.yologram.worker.domain.tech.article.entity.TechArticle
import link.yologram.worker.domain.tech.article.entity.TechArticleSource
import link.yologram.worker.domain.tech.article.repository.TechArticleRepository
import link.yologram.worker.domain.tech.article.repository.TechArticleSourceRepository
import org.springframework.stereotype.Service

private val logger = KotlinLogging.logger {}

@Service
class TechArticleCollectService(
    private val techArticleSourceRepository: TechArticleSourceRepository,
    private val techArticleRepository: TechArticleRepository,
    private val rssFeedClient: RssFeedClient,
) {

    /**
     * 활성 RSS 소스 전체를 순회하며 신규 기사만 저장.
     * 소스 단위로 격리 — 한 소스 실패가 다른 소스 수집을 막지 않는다.
     * link UNIQUE(DDL) + 저장 전 기존 link 배치 조회로 중복 수집 방지 (재실행 멱등).
     */
    fun collect(): CollectResult {
        val sources = techArticleSourceRepository.findByIsActiveTrue()
        val savedArticles = mutableListOf<TechArticle>()
        var failed = 0

        for (source in sources) {
            runCatching { collectFrom(source) }
                .onSuccess { savedArticles += it }
                .onFailure {
                    failed++
                    logger.error(it) { "테크 아티클 수집 실패: source=${source.name}(${source.id}) url=${source.url}" }
                }
        }

        logger.info { "테크 아티클 수집 완료: sources=${sources.size} saved=${savedArticles.size} failedSources=$failed" }
        return CollectResult(sourceCount = sources.size, savedCount = savedArticles.size, failedSourceCount = failed)
    }

    private fun collectFrom(source: TechArticleSource): List<TechArticle> {
        // ① RSS 조회
        val articles = rssFeedClient.fetch(source.url)
        if (articles.isEmpty()) return emptyList()

        // ② 피드 내 중복 제거
        val distinct = articles.distinctBy { it.link }
        val existingLinks = techArticleRepository.findExistingLinks(distinct.map { it.link }).toSet()
        val fresh = distinct.filter { it.link !in existingLinks }  // ③ 기존 저장분 제외
        if (fresh.isEmpty()) return emptyList()

        // ④ DB 저장
        val saved = techArticleRepository.saveAll(
            fresh.map {
                TechArticle(
                    sourceId = source.id,
                    title = it.title,
                    link = it.link,
                    sourceName = source.name,
                    publishedAt = it.publishedAt,
                )
            }
        )
        logger.info { "테크 아티클 수집: source=${source.name} fetched=${articles.size} saved=${saved.size}" }
        return saved
    }

    data class CollectResult(
        val sourceCount: Int,
        val savedCount: Int,
        val failedSourceCount: Int,
    )
}
