package link.yologram.api.v1.domain.pms.tech.entity

import jakarta.persistence.*
import org.springframework.data.annotation.CreatedDate
import org.springframework.data.annotation.LastModifiedDate
import org.springframework.data.jpa.domain.support.AuditingEntityListener
import java.time.LocalDateTime

/**
 * 테크 게시판 게시글. 섹션은 테이블명(tech_post)이 담당 — section 컬럼 없음.
 * 인덱스(idx_tech_post_user_id)는 DDL로만 관리 (엔티티 @Index 선언 금지 — 프로젝트 관례).
 */
@Entity
@Table(name = "tech_post")
@EntityListeners(AuditingEntityListener::class)
class TechPost(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,

    @Column(nullable = false)
    val userId: Long,

    @Column(length = 200)
    var title: String? = null,

    @Column(nullable = false, columnDefinition = "TEXT")
    var content: String,

    @Column(nullable = false)
    var likeCount: Int = 0,

    @Column(nullable = false)
    var commentCount: Int = 0,

    @CreatedDate
    @Column(nullable = false, updatable = false)
    var createdAt: LocalDateTime = LocalDateTime.now(),

    @LastModifiedDate
    @Column(nullable = false)
    var modifiedDate: LocalDateTime = LocalDateTime.now(),
) {
    /** 본인 글 수정: 제목·내용 갱신 (카테고리 매핑은 service에서 교체). modifiedDate는 Auditing이 자동 갱신 */
    fun update(title: String?, content: String) {
        this.title = title
        this.content = content
    }
}
