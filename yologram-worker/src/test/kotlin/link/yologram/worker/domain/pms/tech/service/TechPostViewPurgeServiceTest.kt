package link.yologram.worker.domain.pms.tech.service

import jakarta.persistence.EntityManager
import link.yologram.worker.domain.pms.tech.entity.TechPostView
import link.yologram.worker.domain.pms.tech.repository.TechPostViewRepository
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ActiveProfiles
import org.springframework.transaction.annotation.Transactional
import org.springframework.transaction.support.TransactionOperations
import java.time.Duration
import java.time.LocalDateTime
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class TechPostViewPurgeServiceTest {

    @Autowired
    lateinit var purgeService: TechPostViewPurgeService

    @Autowired
    lateinit var viewRepository: TechPostViewRepository

    @Autowired
    lateinit var entityManager: EntityManager

    /** 정리 기준 시각 — 임계는 이 시각의 30일 전 */
    private val now = LocalDateTime.of(2026, 8, 13, 4, 30, 0)
    private val threshold = now.minusDays(TechPostViewPurgeService.RETENTION_DAYS)

    @BeforeEach
    fun setUp() {
        viewRepository.deleteAll()
        entityManager.flush()
    }

    private fun save(occurredAt: LocalDateTime, viewKey: String) {
        viewRepository.save(TechPostView(postId = 1200, uid = 12, viewKey = viewKey, occurredAt = occurredAt))
    }

    private fun remainingKeys(): Set<String> {
        entityManager.clear()
        return viewRepository.findAll().map { it.viewKey }.toSet()
    }

    @Nested
    inner class 보관_기간_경계 {

        @Test
        fun `30일보다 오래된 이력만 삭제한다`() {
            save(now.minusDays(31), "d31")
            save(now.minusDays(30).minusSeconds(1), "d30-1s")
            save(now.minusDays(29), "d29")
            save(now, "today")
            entityManager.flush()

            val result = purgeService.purge(now)

            assertEquals(threshold, result.threshold)
            assertEquals(2, result.deletedCount)
            assertEquals(setOf("d29", "today"), remainingKeys())
        }

        @Test
        fun `임계 시각과 정확히 같은 행은 남긴다 (경계 포함 안 함)`() {
            save(threshold, "boundary")
            save(threshold.minusSeconds(1), "just-older")
            entityManager.flush()

            val result = purgeService.purge(now)

            assertEquals(1, result.deletedCount)
            assertEquals(setOf("boundary"), remainingKeys())
        }

        @Test
        fun `삭제 대상이 없으면 아무것도 지우지 않는다`() {
            save(now.minusDays(1), "recent")
            entityManager.flush()

            val result = purgeService.purge(now)

            assertEquals(0, result.deletedCount)
            assertEquals(1, result.chunkCount)
            assertEquals(setOf("recent"), remainingKeys())
        }

        @Test
        fun `이력이 비어 있어도 안전하게 종료한다`() {
            val result = purgeService.purge(now)

            assertEquals(0, result.deletedCount)
            assertTrue(remainingKeys().isEmpty())
        }

        @Test
        fun `보관 기간은 Kinesis retention·중복 판정 단위보다 충분히 길다`() {
            // 재처리 중복 방어가 유지되려면 이력이 스트림 보관(24h)·viewDate 단위(1일)보다 오래 남아야 한다
            assertTrue(TechPostViewPurgeService.RETENTION_DAYS > 2)
            assertEquals(30L, TechPostViewPurgeService.RETENTION_DAYS)
        }

        @Test
        fun `기준 시각 기본값은 JVM 기본 TZ다 (occurred_at과 같은 시간축)`() {
            // 전 서비스 TZ를 KST로 통일해 occurred_at도 KST 벽시계다 — 임계도 같은 축이어야 한다.
            // 통일 전에는 producer만 UTC라 임계를 UTC로 잡는 우회가 있었고, 그 우회를 남겨두면
            // 이제는 임계가 9시간 이르게 잡혀 실제 보관이 29일 15시간이 된다
            val repository = mock<TechPostViewRepository>()
            whenever(repository.deleteOlderThan(any(), any())).thenReturn(0)

            val result = TechPostViewPurgeService(repository, TransactionOperations.withoutTransaction()).purge()

            val expected = LocalDateTime.now().minusDays(TechPostViewPurgeService.RETENTION_DAYS)
            // 실행 시각 차이를 허용하되 9시간 오차는 걸러낼 수 있는 폭
            assertTrue(
                Duration.between(result.threshold, expected).abs() < Duration.ofMinutes(1),
                "임계가 JVM 기본 TZ 기준이 아니다: threshold=${result.threshold} expected≈$expected",
            )
        }
    }

    @Nested
    inner class 청크_반복 {

        /** 청크 루프는 대량 행이 필요해 리포지토리를 목으로 검증 (락 구간 분할 계약) */
        private fun serviceWith(repository: TechPostViewRepository) =
            TechPostViewPurgeService(repository, TransactionOperations.withoutTransaction())

        @Test
        fun `청크가 꽉 차면 다음 청크를 이어서 삭제한다`() {
            val repository = mock<TechPostViewRepository>()
            whenever(repository.deleteOlderThan(any(), eq(TechPostViewPurgeService.CHUNK_SIZE)))
                .thenReturn(TechPostViewPurgeService.CHUNK_SIZE, TechPostViewPurgeService.CHUNK_SIZE, 7)

            val result = serviceWith(repository).purge(now)

            assertEquals(TechPostViewPurgeService.CHUNK_SIZE * 2 + 7, result.deletedCount)
            assertEquals(3, result.chunkCount)
            verify(repository, times(3)).deleteOlderThan(eq(threshold), eq(TechPostViewPurgeService.CHUNK_SIZE))
        }

        @Test
        fun `청크를 다 못 채우면 한 번에 종료한다`() {
            val repository = mock<TechPostViewRepository>()
            whenever(repository.deleteOlderThan(any(), any())).thenReturn(3)

            val result = serviceWith(repository).purge(now)

            assertEquals(3, result.deletedCount)
            assertEquals(1, result.chunkCount)
            verify(repository, times(1)).deleteOlderThan(any(), any())
        }

        @Test
        fun `삭제가 끝나지 않아도 회차 상한에서 멈춘다 (무한 루프 방지)`() {
            val repository = mock<TechPostViewRepository>()
            whenever(repository.deleteOlderThan(any(), any())).thenReturn(TechPostViewPurgeService.CHUNK_SIZE)

            val result = serviceWith(repository).purge(now)

            assertEquals(TechPostViewPurgeService.MAX_CHUNKS_PER_RUN, result.chunkCount)
            verify(repository, times(TechPostViewPurgeService.MAX_CHUNKS_PER_RUN)).deleteOlderThan(any(), any())
        }
    }
}
