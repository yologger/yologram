package link.yologram.api.v1.domain.comment.repository

import link.yologram.api.v1.domain.comment.entity.Comment
import org.springframework.data.jpa.repository.JpaRepository

interface CommentRepository : JpaRepository<Comment, Long>
