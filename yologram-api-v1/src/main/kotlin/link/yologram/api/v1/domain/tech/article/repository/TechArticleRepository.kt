package link.yologram.api.v1.domain.tech.article.repository

import link.yologram.api.v1.domain.tech.article.entity.TechArticle
import org.springframework.data.jpa.repository.JpaRepository

interface TechArticleRepository : JpaRepository<TechArticle, Long>, TechArticleRepositoryCustom
