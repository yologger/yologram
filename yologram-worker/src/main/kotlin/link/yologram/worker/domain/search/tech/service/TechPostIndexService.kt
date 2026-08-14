package link.yologram.worker.domain.search.tech.service

import io.github.oshai.kotlinlogging.KotlinLogging
import jakarta.annotation.PostConstruct
import link.yologram.worker.domain.search.tech.document.TechPostDocument
import link.yologram.worker.infra.client.pms.PmsApiClient
import org.opensearch.client.opensearch.OpenSearchClient
import org.opensearch.client.opensearch._types.mapping.TypeMapping
import org.opensearch.client.opensearch.core.BulkRequest
import org.opensearch.client.opensearch.indices.IndexSettings
import org.opensearch.client.opensearch.indices.PutIndicesSettingsRequest
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.core.io.ClassPathResource
import org.springframework.stereotype.Service
import java.io.StringReader

/**
 * 게시글 검색 인덱싱 — SQS로 받은 범위를 DB에서 읽어 OpenSearch에 bulk 색인한다.
 * (레거시 BoardIndexingService의 rangeIndexing + 템플릿·alias 관리 미러)
 *
 * 인덱스는 버전 인덱스 + alias 구조다:
 *   tech-post-index-v1   실제 인덱스
 *   tech-post-index      alias — 색인·검색 모두 이 이름만 쓴다
 * 매핑을 바꿔야 하면 v2를 만들어 재색인한 뒤 alias를 옮기면 무중단 전환이 된다.
 * 애플리케이션이 실제 인덱스명을 직접 쓰지 않는 것이 이 전략의 전제다.
 *
 * 기동 시 인덱스·alias를 upsert한다(레거시 @PostConstruct 패턴) — 첫 색인 전에 매핑이 반드시 존재해야
 * 동적 매핑으로 title/content가 nori 없이 잡히는 사고를 막는다.
 */
@Service
@ConditionalOnProperty(prefix = "opensearch.main", name = ["enabled"], havingValue = "true")
class TechPostIndexService(
    private val client: OpenSearchClient,
    private val pmsApiClient: PmsApiClient,
) {

    private val logger = KotlinLogging.logger {}

    @PostConstruct
    fun initIndex() {
        runCatching { createIndexWithAliasIfAbsent() }
            .onFailure {
                // 인덱스 준비 실패가 워커 기동을 막지 않게 한다 — 뉴스 파이프라인은 계속 돌아야 한다.
                // 색인 시점에 인덱스가 없으면 OpenSearch가 오류를 내므로 조용히 잘못 색인될 위험은 없다
                logger.error(it) { "failed to prepare index [$INDEX_NAME] — indexing will fail until resolved" }
            }
    }

    /**
     * 범위 색인. 반환값은 실제 색인한 문서 수.
     * 삭제된 id 구간이면 조회 결과가 비어 0을 반환한다(정상 — 풀 인덱싱은 id 공백을 그냥 지나간다).
     */
    fun index(from: Long, to: Long): Int {
        val posts = pmsApiClient.findPostsForIndex(from, to)
        if (posts.isEmpty()) {
            logger.info { "no posts to index in range [$from, $to]" }
            return 0
        }

        var indexed = 0
        // 한 bulk 요청이 지나치게 커지지 않게 나눈다 — 요청 크기가 크면 OpenSearch 힙(512m)에 부담이 된다
        posts.chunked(BULK_CHUNK_SIZE).forEach { chunk ->
            val request = BulkRequest.Builder().apply {
                chunk.forEach { post ->
                    val document = TechPostDocument.of(post)
                    operations { op ->
                        op.index { idx ->
                            idx.index(INDEX_ALIAS)
                                // 문서 id를 게시글 id로 고정 — 같은 글을 다시 색인하면 덮어쓴다(멱등).
                                // SQS 중복 전달·재시도가 문서를 늘리지 않는 근거다
                                .id(post.id.toString())
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

        logger.info { "indexed posts range=[$from, $to] count=$indexed" }
        return indexed
    }

    /**
     * 인덱스가 없으면 settings(nori analyzer)·mappings와 함께 만들고 alias를 붙인다.
     * 이미 있으면 건드리지 않는다 — 매핑 변경은 새 버전 인덱스 + 재색인으로 처리할 일이지
     * 기동 때마다 덮어쓸 일이 아니다(기존 문서의 매핑은 어차피 바뀌지 않는다).
     */
    private fun createIndexWithAliasIfAbsent() {
        val exists = client.indices().exists { it.index(INDEX_NAME) }.value()
        if (exists) {
            logger.info { "index already exists: $INDEX_NAME" }
            return
        }

        val settings = readResource("opensearch/tech-post/settings.json")
        val mappings = readResource("opensearch/tech-post/mappings.json")

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
        const val INDEX_ALIAS = "tech-post-index"
        const val INDEX_VERSION = "v1"
        const val INDEX_NAME = "$INDEX_ALIAS-$INDEX_VERSION"

        /** bulk 한 요청에 담을 문서 수 (레거시 BULK_INDEXING_BATCH_SIZE=5보다 크게 — 5는 실서비스엔 과도하게 작다) */
        const val BULK_CHUNK_SIZE = 100
    }
}
