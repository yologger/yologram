package link.yologram.api.v1.domain.pms.repository

import link.yologram.api.v1.domain.pms.entity.PostCategory
import org.springframework.data.jpa.repository.JpaRepository

interface PostCategoryRepository : JpaRepository<PostCategory, Long>
