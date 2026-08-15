package link.yologram.api.v1.domain.search.tech.document

import java.time.LocalDateTime

/**
 * 색인된 뉴스 문서 (읽기 전용) — worker TechNewsDocument와 같은 스키마다.
 * 두 프로젝트가 문자열 계약으로 맞물리므로 필드를 바꾸면 양쪽을 동시에 고쳐야 한다.
 *
 * 카테고리는 id만 색인한다(categoryIds) — 라벨은 tech_category 마스터에서 바뀔 수 있어
 * 색인에 넣으면 이름이 바뀔 때마다 재색인이 필요하다. 응답 조립 시 cms에서 배치 해석한다
 * (TechNewsSearchService — 게시글 검색이 닉네임을 채우는 것과 같은 이유).
 */
data class TechNewsDocument(
    val id: Long = 0,
    val title: String = "",
    val summary: String = "",
    val link: String = "",
    val sourceName: String = "",
    val categoryIds: List<Long> = emptyList(),
    val publishedAt: LocalDateTime? = null,
    val createdAt: LocalDateTime? = null,
)
