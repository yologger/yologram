package link.yologram.api.v1.domain.pms.tech.repository

import link.yologram.api.v1.domain.pms.tech.entity.TechPost
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query

interface TechPostRepository : JpaRepository<TechPost, Long>, TechPostRepositoryCustom {

    /**
     * 최대 게시글 id — 풀 인덱싱이 훑을 범위의 상한.
     * count가 아니라 max(id)를 쓰는 이유: 삭제로 생긴 id 공백 때문에 count로는 끝을 알 수 없다.
     * 글이 하나도 없으면 null.
     */
    @Query("select max(p.id) from TechPost p")
    fun findMaxId(): Long?
}
