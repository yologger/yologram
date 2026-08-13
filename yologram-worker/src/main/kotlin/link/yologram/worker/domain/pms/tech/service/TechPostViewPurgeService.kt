package link.yologram.worker.domain.pms.tech.service

import io.github.oshai.kotlinlogging.KotlinLogging
import link.yologram.worker.domain.pms.tech.repository.TechPostViewRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.support.TransactionOperations
import java.time.LocalDateTime
import java.time.ZoneOffset

private val logger = KotlinLogging.logger {}

@Service
class TechPostViewPurgeService(
    private val techPostViewRepository: TechPostViewRepository,
    private val transactionOperations: TransactionOperations,
) {

    /**
     * 보관 기간(RETENTION_DAYS)이 지난 조회 이력을 청크 단위로 삭제.
     *
     * 보관 기간을 30일로 잡은 근거: 이력은 중복 방어 장치이므로
     * Kinesis retention(24시간)과 중복 판정 단위(viewDate, 1일)보다 충분히 길어야 한다.
     * 24시간 안에 재전달된 레코드는 이력에 그 키가 아직 남아 있어야 흡수되고,
     * 날짜 경계 직전 이벤트까지 감안하면 최소 며칠이면 되지만
     * 사후 분석(uid·ip)과 카운트 재계산 복구 여지를 위해 여유를 둔 값이다.
     * 30일보다 짧게 줄이면 재처리 중복 방어와 복구 근거가 함께 약해진다.
     *
     * 청크 반복 이유: 한 번에 대량 DELETE를 날리면 그 사이 조회 이벤트 소비가
     * 같은 테이블 락을 기다리게 된다. chunkSize씩 각각 별도 트랜잭션으로 커밋해 락 구간을 짧게 끊는다
     * (Fargate Spot 중단으로 중간에 끊겨도 이미 커밋된 청크는 유효 — 다음 회차가 이어서 지운다).
     *
     * 기준 시각은 UTC로 잡는다 — 비교 대상 occurred_at이 producer(api-v1·api-v2, 컨테이너 TZ 미설정)
     * 기준 UTC 벽시계 값이기 때문이다. 워커 JVM 기본 TZ(Asia/Seoul)로 now()를 만들면 임계가 9시간
     * 미래로 잡혀 실제 보관이 30일이 아니라 29일 15시간이 된다.
     */
    fun purge(now: LocalDateTime = LocalDateTime.now(ZoneOffset.UTC)): PurgeResult {
        val threshold = now.minusDays(RETENTION_DAYS)
        var deleted = 0
        var chunks = 0

        while (chunks < MAX_CHUNKS_PER_RUN) {
            val affected = transactionOperations.execute {
                techPostViewRepository.deleteOlderThan(threshold, CHUNK_SIZE)
            } ?: 0

            deleted += affected
            chunks++

            // 청크를 다 못 채웠으면 남은 대상이 없다 — 종료
            if (affected < CHUNK_SIZE) break
        }

        // 0건도 찍는다 — 하루 1회라 노이즈가 없고, 스케줄러가 살아 있다는 확인이 된다
        logger.info { "게시글 조회 이력 정리: ${deleted}건 삭제" }
        return PurgeResult(threshold = threshold, deletedCount = deleted, chunkCount = chunks)
    }

    data class PurgeResult(
        val threshold: LocalDateTime,
        val deletedCount: Int,
        val chunkCount: Int,
    )

    companion object {
        /** 이력 보관 기간(일) */
        const val RETENTION_DAYS = 30L

        /** 한 청크에서 삭제할 최대 행 수 */
        const val CHUNK_SIZE = 1000

        /**
         * 1회 실행의 청크 상한 — 삭제가 끝나지 않아도 회차를 마친다.
         * 무한 루프 방지 + 하루 1회 실행이라 남은 분량은 다음 회차가 이어서 처리한다.
         */
        const val MAX_CHUNKS_PER_RUN = 100
    }
}
