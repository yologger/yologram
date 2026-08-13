package link.yologram.api.v1.domain.pms.tech.repository

import link.yologram.api.v1.domain.pms.tech.entity.TechPostLike
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param

interface TechPostLikeRepository : JpaRepository<TechPostLike, Long> {

    /**
     * 좋아요 이력 삽입 (멱등). 반환값 = 실제 삽입된 행 수 —
     * 이미 (post_id, uid)가 있으면 INSERT IGNORE가 uk 충돌을 무시하고 0을 반환한다.
     * 동시 요청도 한쪽만 1을 받아 카운트 증가가 정확히 1회로 수렴.
     * save+flush 후 uk 예외를 잡는 방식은 Hibernate 세션이 오염돼(예외 후 세션 사용 불가)
     * 같은 트랜잭션의 카운트 갱신이 깨지므로 네이티브 한 문장으로 처리.
     */
    @Modifying
    @Query(
        value = "INSERT IGNORE INTO tech_post_like (post_id, uid, created_at) VALUES (:postId, :uid, NOW(6))",
        nativeQuery = true,
    )
    fun insertIgnore(@Param("postId") postId: Long, @Param("uid") uid: Long): Int

    /**
     * 좋아요 이력 삭제 (멱등). 반환값 = 실제 삭제된 행 수 —
     * 안 누른 상태면 0 (호출부가 카운트 감소를 건너뛴다). JPQL 벌크 delete 한 문장.
     */
    @Modifying
    @Query("DELETE FROM TechPostLike l WHERE l.postId = :postId AND l.uid = :uid")
    fun deleteByPostIdAndUid(@Param("postId") postId: Long, @Param("uid") uid: Long): Int

    /** likedByMe 단건 (상세 조회용) — 로그인 유저가 이 글에 좋아요를 눌렀는지 */
    fun existsByPostIdAndUid(postId: Long, uid: Long): Boolean

    /** likedByMe 배치 (목록 조회용) — 로그인 유저가 누른 글만 추려 postId Set으로 (N+1 회피) */
    fun findByUidAndPostIdIn(uid: Long, postIds: List<Long>): List<TechPostLike>
}
