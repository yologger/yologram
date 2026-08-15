package link.yologram.worker.domain.search.tech.service

import io.github.oshai.kotlinlogging.KotlinLogging
import jakarta.annotation.PostConstruct
import link.yologram.worker.domain.news.tech.entity.TechNews
import link.yologram.worker.domain.news.tech.enums.TechNewsStatus
import link.yologram.worker.domain.news.tech.repository.TechNewsCategoryMappingRepository
import link.yologram.worker.domain.news.tech.repository.TechNewsRepository
import link.yologram.worker.domain.search.tech.document.TechNewsDocument
import org.opensearch.client.opensearch.OpenSearchClient
import org.opensearch.client.opensearch._types.mapping.TypeMapping
import org.opensearch.client.opensearch.core.BulkRequest
import org.opensearch.client.opensearch.indices.IndexSettings
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.core.io.ClassPathResource
import org.springframework.stereotype.Service
import java.io.StringReader

/**
 * 뉴스 검색 인덱싱 — 게시글(TechPostIndexService)과 같은 구조다.
 *
 * 색인 경로가 둘이다:
 *   실시간 — 요약 배치가 끝나면 그 배치에서 SUMMARIZED가 된 건들을 직접 색인(index(ids))
 *   배치   — 어드민이 요청한 범위를 SQS로 받아 색인(index(from, to))
 * 문서 id를 뉴스 id로 고정해 두 경로가 겹쳐도 덮어쓰기가 된다(멱등).
 *
 * 뉴스는 worker가 소유한 도메인이라 리포지토리를 직접 쓴다 —
 * 게시글이 infra/client/pms를 경유한 것은 pms가 api 소유라 경계를 넘기 때문이다.
 *
 * 색인 대상은 SUMMARIZED만이다. COLLECTED는 summary가 없어 검색에 걸려도 보여줄 내용이 없고,
 * FAILED는 요약을 포기한 건이다.
 */
@Service
@ConditionalOnProperty(prefix = "opensearch.main", name = ["enabled"], havingValue = "true")
class TechNewsIndexService(
    private val client: OpenSearchClient,
    private val techNewsRepository: TechNewsRepository,
    private val categoryMappingRepository: TechNewsCategoryMappingRepository,
) {

    private val logger = KotlinLogging.logger {}

    @PostConstruct
    fun initIndex() {
        runCatching { createIndexWithAliasIfAbsent() }
            .onFailure {
                // 인덱스 준비 실패가 워커 기동을 막지 않게 한다 (게시글 인덱스와 같은 판단)
                logger.error(it) { "failed to prepare index [$INDEX_NAME] — indexing will fail until resolved" }
            }
    }

    /** 범위 색인 — SQS로 받은 작업. 반환값은 실제 색인한 문서 수 */
    fun index(from: Long, to: Long): Int {
        val newsList = techNewsRepository.findByIdBetweenAndStatusOrderByIdAsc(from, to, TechNewsStatus.SUMMARIZED)
        if (newsList.isEmpty()) {
            logger.info { "no news to index in range [$from, $to]" }
            return 0
        }
        return indexAll(newsList).also { logger.info { "indexed news range=[$from, $to] count=$it" } }
    }

    /**
     * id 목록 색인 — 요약 배치가 끝난 뒤 호출한다.
     * 배치 끝에 한 번만 부르는 이유: bulk 왕복이 건별 N회가 아니라 1회가 되고,
     * 건별 트랜잭션이 모두 커밋된 뒤라 색인 시점에 DB와 어긋나지 않는다
     * (요약 배치의 캐시 무효화와 같은 원칙 — 배치당 1회·커밋 이후).
     */
    fun index(ids: List<Long>): Int {
        if (ids.isEmpty()) return 0
        val newsList = techNewsRepository.findByIdInAndStatus(ids, TechNewsStatus.SUMMARIZED)
        if (newsList.isEmpty()) return 0
        return indexAll(newsList).also { logger.info { "indexed news ids=${ids.size} count=$it" } }
    }

    private fun indexAll(newsList: List<TechNews>): Int {
        // 카테고리는 1:N이라 별도 배치 조회 후 newsId로 묶는다 (N+1 회피 — 게시글과 같은 방식)
        val categoryIdsByNews = categoryMappingRepository.findByNewsIdIn(newsList.map { it.id })
            .groupBy({ it.newsId }, { it.categoryId })

        var indexed = 0
        // 한 bulk 요청이 지나치게 커지지 않게 나눈다 — 요청이 크면 OpenSearch 힙(512m)에 부담이 된다
        newsList.chunked(BULK_CHUNK_SIZE).forEach { chunk ->
            val request = BulkRequest.Builder().apply {
                chunk.forEach { news ->
                    val document = TechNewsDocument.of(news, categoryIdsByNews[news.id].orEmpty())
                    operations { op ->
                        op.index { idx ->
                            idx.index(INDEX_ALIAS)
                                // 문서 id를 뉴스 id로 고정 — 재색인·중복 전달이 덮어쓰기가 된다(멱등)
                                .id(news.id.toString())
                                .document(document)
                        }
                    }
                }
            }.build()

            val response = client.bulk(request)
            if (response.errors()) {
                val failures = response.items().mapNotNull { it.error()?.reason() }.take(3)
                logger.error { "bulk indexing had failures: ${failures.joinToString(" | ")}" }
            } else {
                indexed += chunk.size
            }
        }
        return indexed
    }

    /**
     * 인덱스가 없으면 settings(nori analyzer)·mappings와 함께 만들고 alias를 붙인다.
     * 이미 있으면 건드리지 않는다 — 매핑 변경은 새 버전 인덱스 + 재색인으로 처리한다.
     */
    private fun createIndexWithAliasIfAbsent() {
        val exists = client.indices().exists { it.index(INDEX_NAME) }.value()
        if (exists) {
            logger.info { "index already exists: $INDEX_NAME" }
            return
        }

        val settings = readResource("opensearch/tech-news/settings.json")
        val mappings = readResource("opensearch/tech-news/mappings.json")

        client.indices().create { create ->
            create.index(INDEX_NAME)
                .settings(IndexSettings.Builder().withJson(StringReader(settings)).build())
                .mappings(TypeMapping.Builder().withJson(StringReader(mappings)).build())
                .aliases(INDEX_ALIAS) { it.isWriteIndex(true) }
        }
        logger.info { "created index [$INDEX_NAME] with alias [$INDEX_ALIAS]" }
    }

    private fun readResource(path: String): String =
        ClassPathResource(path).inputStream.bufferedReader().use { it.readText() }

    companion object {
        const val INDEX_ALIAS = "tech-news-index"

        /** title·summary에 처음부터 standard 서브필드를 넣었다 — 게시글이 v2로 올라간 이유(외래어)를 반복하지 않는다 */
        const val INDEX_VERSION = "v1"
        const val INDEX_NAME = "$INDEX_ALIAS-$INDEX_VERSION"

        const val BULK_CHUNK_SIZE = 100
    }
}
