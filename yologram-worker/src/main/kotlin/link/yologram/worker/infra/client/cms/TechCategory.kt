package link.yologram.worker.infra.client.cms

import jakarta.persistence.*

/**
 * 테크 카테고리 마스터 (tech_category — cms 도메인 소유, 어드민이 관리).
 * cms 소유 데이터의 읽기 모델 — client 층 소유. 도메인 코드는 CmsApiClient를 경유해서만 접근한다.
 * worker는 조회 전용: 활성 카테고리가 LLM 분류 어휘가 된다.
 */
@Entity
@Table(name = "tech_category")
class TechCategory(
    @Id
    val id: Long = 0,

    @Column(nullable = false, length = 50)
    val name: String,

    @Column(nullable = false)
    val sortOrder: Int = 0,

    @Column(nullable = false)
    val isActive: Boolean = true,
)
