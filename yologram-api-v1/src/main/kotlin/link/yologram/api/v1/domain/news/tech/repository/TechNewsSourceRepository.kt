package link.yologram.api.v1.domain.news.tech.repository

import link.yologram.api.v1.domain.news.tech.entity.TechNewsSource
import org.springframework.data.jpa.repository.JpaRepository

interface TechNewsSourceRepository : JpaRepository<TechNewsSource, Long> {
    fun findAllByOrderByIdAsc(): List<TechNewsSource>
    fun existsByUrl(url: String): Boolean
    fun existsByUrlAndIdNot(url: String, id: Long): Boolean
}
