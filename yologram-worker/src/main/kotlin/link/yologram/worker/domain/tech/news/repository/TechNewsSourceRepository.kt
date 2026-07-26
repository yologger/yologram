package link.yologram.worker.domain.tech.article.repository

import link.yologram.worker.domain.tech.article.entity.TechArticleSource
import org.springframework.data.jpa.repository.JpaRepository

interface TechArticleSourceRepository : JpaRepository<TechArticleSource, Long> {
    fun findByIsActiveTrue(): List<TechArticleSource>
}
