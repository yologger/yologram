package link.yologram.api.v1.domain.news.tech.repository

import link.yologram.api.v1.domain.news.tech.entity.TechNews
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query

interface TechNewsRepository : JpaRepository<TechNews, Long>, TechNewsRepositoryCustom {

    /**
     * 전체 인덱싱 범위의 상한.
     * count가 아니라 max(id)를 쓰는 이유: 삭제로 생긴 id 공백 때문에 count로는 끝을 알 수 없다.
     * 뉴스가 하나도 없으면 null.
     */
    @Query("select max(n.id) from TechNews n")
    fun findMaxId(): Long?
}
