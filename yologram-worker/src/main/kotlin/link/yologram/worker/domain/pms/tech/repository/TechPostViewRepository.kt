package link.yologram.worker.domain.pms.tech.repository

import link.yologram.worker.domain.pms.tech.entity.TechPostView
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import java.time.LocalDateTime

interface TechPostViewRepository : JpaRepository<TechPostView, Long> {

    /** 이미 적재된 view_key만 골라 반환 — 배치 신규분 필터링용 (uk 인덱스 IN 조회) */
    @Query("select v.viewKey from TechPostView v where v.viewKey in :viewKeys")
    fun findExistingViewKeys(@Param("viewKeys") viewKeys: Collection<String>): List<String>

    /**
     * 조회 이력 삽입 (멱등). 반환값 = 실제 삽입된 행 수 —
     * 이미 같은 view_key가 있으면 INSERT IGNORE가 uk 충돌을 무시하고 0을 반환한다.
     * 이 반환값(1/0)만으로 카운트 delta를 산정하므로 재전달·중복 발행이 카운트를 부풀리지 않는다.
     *
     * save+flush 후 uk 예외를 잡는 방식은 Hibernate 세션이 오염돼(예외 후 세션 사용 불가)
     * 같은 트랜잭션의 카운트 갱신이 깨지므로 네이티브 한 문장으로 처리
     * (api-v1 TechPostLikeRepository.insertIgnore와 동일한 이유).
     */
    @Modifying
    @Query(
        value = "INSERT IGNORE INTO tech_post_view (post_id, uid, ip, view_key, occurred_at, created_at) " +
            "VALUES (:postId, :uid, :ip, :viewKey, :occurredAt, NOW(6))",
        nativeQuery = true,
    )
    fun insertIgnore(
        @Param("postId") postId: Long,
        @Param("uid") uid: Long?,
        @Param("ip") ip: String?,
        @Param("viewKey") viewKey: String,
        @Param("occurredAt") occurredAt: LocalDateTime,
    ): Int

    /**
     * 보관 기간 경과분 청크 삭제. 반환값 = 삭제된 행 수 —
     * 호출부가 chunkSize 미만을 받을 때까지 반복해 대량 DELETE로 락을 오래 잡지 않게 한다.
     * JPQL은 LIMIT을 지원하지 않아 네이티브 (MySQL DELETE ... LIMIT).
     * 기준 컬럼은 occurred_at — 멱등 판정(viewDate)과 같은 시간축이라 "언제까지 중복 방어가 유지되는가"를 직접 읽을 수 있다.
     */
    @Modifying
    @Query(
        value = "DELETE FROM tech_post_view WHERE occurred_at < :threshold LIMIT :chunkSize",
        nativeQuery = true,
    )
    fun deleteOlderThan(
        @Param("threshold") threshold: LocalDateTime,
        @Param("chunkSize") chunkSize: Int,
    ): Int
}
