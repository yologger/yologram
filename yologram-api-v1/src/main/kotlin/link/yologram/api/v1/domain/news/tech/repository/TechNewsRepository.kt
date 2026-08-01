package link.yologram.api.v1.domain.news.tech.repository

import link.yologram.api.v1.domain.news.tech.entity.TechNews
import org.springframework.data.jpa.repository.JpaRepository

interface TechNewsRepository : JpaRepository<TechNews, Long>, TechNewsRepositoryCustom
