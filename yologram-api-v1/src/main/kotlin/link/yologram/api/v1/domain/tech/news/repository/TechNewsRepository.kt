package link.yologram.api.v1.domain.tech.news.repository

import link.yologram.api.v1.domain.tech.news.entity.TechNews
import org.springframework.data.jpa.repository.JpaRepository

interface TechNewsRepository : JpaRepository<TechNews, Long>, TechNewsRepositoryCustom
