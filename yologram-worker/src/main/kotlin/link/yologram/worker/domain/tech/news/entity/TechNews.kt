package link.yologram.worker.domain.tech.news.entity

import jakarta.persistence.*
import link.yologram.worker.domain.tech.news.enums.TechNewsStatus
import org.springframework.data.annotation.CreatedDate
import org.springframework.data.annotation.LastModifiedDate
import org.springframework.data.jpa.domain.support.AuditingEntityListener
import java.time.LocalDateTime

@Entity
@Table(name = "tech_news")
@EntityListeners(AuditingEntityListener::class)
class TechNews(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,

    // 수집 소스 (tech_news_source.id — 프로젝트 관례대로 FK 없이 컬럼+인덱스)
    @Column(nullable = false)
    val sourceId: Long,

    @Column(nullable = false, length = 500)
    var title: String,

    // 기사 원문 URL — DDL UNIQUE로 중복 수집 방지. LLM 요약 입력은 이 link의 원문 크롤링으로 확보
    @Column(nullable = false, length = 500)
    val link: String,

    // LLM 한국어 요약 (요약 스텝에서 채움)
    @Column(columnDefinition = "text")
    var summary: String? = null,

    @Column(nullable = false, length = 100)
    val sourceName: String,

    @Column(nullable = false)
    val publishedAt: LocalDateTime,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    var status: TechNewsStatus = TechNewsStatus.COLLECTED,

    @Column(nullable = false)
    var retryCount: Int = 0,

    @CreatedDate
    @Column(nullable = false, updatable = false)
    var createdAt: LocalDateTime = LocalDateTime.now(),

    @LastModifiedDate
    @Column(nullable = false)
    var modifiedDate: LocalDateTime = LocalDateTime.now(),
)
