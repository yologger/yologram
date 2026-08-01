package link.yologram.worker.domain.tech.news.repository

import link.yologram.worker.domain.tech.news.entity.TechNewsSource
import org.springframework.data.jpa.repository.JpaRepository

interface TechNewsSourceRepository : JpaRepository<TechNewsSource, Long> {
    fun findByIsActiveTrue(): List<TechNewsSource>
}
