package link.yologram.api.v1.domain.pms.tech.repository

import link.yologram.api.v1.domain.pms.tech.entity.TechPost
import org.springframework.data.jpa.repository.JpaRepository

interface TechPostRepository : JpaRepository<TechPost, Long>, TechPostRepositoryCustom
