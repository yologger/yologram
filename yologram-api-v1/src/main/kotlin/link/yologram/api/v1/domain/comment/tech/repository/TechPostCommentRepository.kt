package link.yologram.api.v1.domain.comment.tech.repository

import link.yologram.api.v1.domain.comment.tech.entity.TechPostComment
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param

interface TechPostCommentRepository : JpaRepository<TechPostComment, Long>, TechPostCommentRepositoryCustom {
    /**
     * 게시글 삭제 시 해당 글의 댓글 전체 제거 (고아 댓글 방지).
     * @Modifying 벌크 delete — 댓글 N건을 개별 delete 대신 쿼리 한 번으로 정리.
     */
    @Modifying
    @Query("delete from TechPostComment c where c.postId = :postId")
    fun deleteByPostId(@Param("postId") postId: Long)
}
