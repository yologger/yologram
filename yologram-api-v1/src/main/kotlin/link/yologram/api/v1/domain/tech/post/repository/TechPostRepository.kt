package link.yologram.api.v1.domain.tech.post.repository

import link.yologram.api.v1.domain.tech.post.entity.TechPost
import org.springframework.data.jpa.repository.JpaRepository

interface TechPostRepository : JpaRepository<TechPost, Long>, TechPostRepositoryCustom
