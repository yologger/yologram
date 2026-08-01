package link.yologram.worker.domain.news.tech.repository

import link.yologram.worker.domain.news.tech.entity.TechNewsSource
import org.springframework.data.jpa.repository.JpaRepository

interface TechNewsSourceRepository : JpaRepository<TechNewsSource, Long> {
    fun findByIsActiveTrue(): List<TechNewsSource>
}
