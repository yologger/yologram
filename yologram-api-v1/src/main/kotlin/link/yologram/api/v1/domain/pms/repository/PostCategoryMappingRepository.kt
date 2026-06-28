package link.yologram.api.v1.domain.pms.repository

import link.yologram.api.v1.domain.pms.entity.PostCategoryMapping
import org.springframework.data.jpa.repository.JpaRepository

interface PostCategoryMappingRepository : JpaRepository<PostCategoryMapping, Long> {
    fun findByPostId(postId: Long): List<PostCategoryMapping>

    fun findByPostIdIn(postIds: Collection<Long>): List<PostCategoryMapping>

    /** 게시글 수정/삭제 시 카테고리 매핑 전체 제거 (수정은 제거 후 재생성으로 교체) */
    fun deleteByPostId(postId: Long)
}
