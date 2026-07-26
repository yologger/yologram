package link.yologram.api.v1.domain.tech.news.entity

import jakarta.persistence.*
import link.yologram.api.v1.domain.tech.news.enums.TechNewsStatus
import java.time.LocalDateTime

/**
 * 테크 뉴스 — worker가 수집·요약해 쌓는 tech_news 테이블의 조회 전용 매핑.
 * api-v1은 읽기만 하므로 전 필드 val (쓰기·상태 전이는 worker 소관).
 */
@Entity
@Table(name = "tech_news")
class TechNews(
    @Id
    val id: Long = 0,

    @Column(nullable = false)
    val sourceId: Long,

    @Column(nullable = false, length = 500)
    val title: String,

    @Column(nullable = false, length = 500)
    val link: String,

    // LLM 한국어 요약 (COLLECTED 단계에선 null)
    @Column(columnDefinition = "text")
    val summary: String? = null,

    @Column(nullable = false, length = 100)
    val sourceName: String,

    @Column(nullable = false)
    val publishedAt: LocalDateTime,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    val status: TechNewsStatus,

    @Column(nullable = false, updatable = false)
    val createdAt: LocalDateTime = LocalDateTime.now(),
)
