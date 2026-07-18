package link.yologram.api.v1.domain.tech.post.repository

import link.yologram.api.v1.domain.tech.post.entity.TechPostCategoryMapping
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param

interface TechPostCategoryMappingRepository : JpaRepository<TechPostCategoryMapping, Long> {
    fun findByPostId(postId: Long): List<TechPostCategoryMapping>

    fun findByPostIdIn(postIds: Collection<Long>): List<TechPostCategoryMapping>

    /**
     * 게시글 수정/삭제 시 카테고리 매핑 전체 제거 (수정은 제거 후 재생성으로 교체).
     * @Modifying 벌크 delete로 즉시 실행 — derived deleteBy는 flush 순서상 insert가 먼저 나가
     * unique(post_id, category_id) 충돌(1062)이 생기므로, 삭제를 즉시 반영한 뒤 재삽입한다.
     */
    @Modifying
    @Query("delete from TechPostCategoryMapping m where m.postId = :postId")
    fun deleteByPostId(@Param("postId") postId: Long)
}
