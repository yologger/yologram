package link.yologram.worker.domain.pms.tech.service

import io.github.oshai.kotlinlogging.KotlinLogging
import link.yologram.worker.domain.pms.tech.subscriber.event.PostViewEvent
import link.yologram.worker.domain.pms.tech.repository.TechPostViewCountRepository
import link.yologram.worker.domain.pms.tech.repository.TechPostViewRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

private val logger = KotlinLogging.logger {}

@Service
class TechPostViewIngestService(
    private val techPostViewRepository: TechPostViewRepository,
    private val techPostViewCountRepository: TechPostViewCountRepository,
) {

    @Transactional
    fun ingest(events: List<PostViewEvent>): IngestResult {
        if (events.isEmpty()) return IngestResult(received = 0, inserted = 0, updatedPostCount = 0)

        // ① 배치 내 중복 제거 — 같은 view_key는 첫 이벤트만 남긴다(어느 것을 남겨도 판정은 동일)
        val distinct = events.associateByTo(LinkedHashMap()) { PostViewKeyFactory.create(it) }

        // ② 이미 적재된 키 제외
        val existing = techPostViewRepository.findExistingViewKeys(distinct.keys).toSet()

        // ③ 신규만 삽입 — 실제 삽입 행 수로 postId별 delta 집계
        val deltaByPostId = LinkedHashMap<Long, Long>()
        var inserted = 0
        for ((viewKey, event) in distinct) {
            if (viewKey in existing) continue

            val affected = techPostViewRepository.insertIgnore(
                postId = event.postId,
                uid = event.uid,
                ip = event.ip,
                viewKey = viewKey,
                occurredAt = event.occurredAt,
            )
            if (affected > 0) {
                inserted += affected
                deltaByPostId.merge(event.postId, affected.toLong(), Long::plus)
            }
        }

        // ④ postId당 카운트 1회 갱신
        for ((postId, delta) in deltaByPostId) {
            techPostViewCountRepository.increase(postId, delta)
        }

        logger.info {
            "게시글 조회 집계: received=${events.size} distinct=${distinct.size} " +
                "inserted=$inserted posts=${deltaByPostId.size}"
        }
        return IngestResult(
            received = events.size,
            inserted = inserted,
            updatedPostCount = deltaByPostId.size,
        )
    }

    data class IngestResult(
        /** 배치로 넘어온 이벤트 수 (파싱 성공분) */
        val received: Int,
        /** 이력에 실제로 적재된 신규 조회 수 = 카운트 증가 총합 */
        val inserted: Int,
        /** 카운트가 갱신된 게시글 수 */
        val updatedPostCount: Int,
    )
}
