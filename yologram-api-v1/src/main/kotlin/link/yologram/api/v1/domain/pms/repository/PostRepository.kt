package link.yologram.api.v1.domain.pms.repository

import link.yologram.api.v1.domain.pms.entity.Post
import org.springframework.data.jpa.repository.JpaRepository

interface PostRepository : JpaRepository<Post, Long>, PostRepositoryCustom
