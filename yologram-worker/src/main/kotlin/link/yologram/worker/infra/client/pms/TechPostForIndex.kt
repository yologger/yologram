package link.yologram.worker.infra.client.pms

import java.time.LocalDateTime

/**
 * 검색 인덱싱용 게시글 읽기 모델 — pms 소유 테이블(tech_post 및 카운트·카테고리 매핑)의 스냅샷.
 *
 * 인덱싱은 pms 데이터를 검색용으로 복제하는 작업이라 경계를 넘는 것이 본질이다.
 * 그 접근을 infra/client 층에만 두어 도메인 코드가 타 도메인 리포지토리를 직접 보지 않게 한다
 * (CmsApiClient가 tech_category를 읽는 것과 같은 규칙).
 *
 * 닉네임은 담지 않는다 — 문서에 넣으면 닉네임 변경 시 그 사용자의 모든 글을 재색인해야 한다.
 * 검색 응답의 닉네임은 api가 기존 Valkey 캐시(ums:users:v1:nickname:{uid})로 채운다.
 */
data class TechPostForIndex(
    val id: Long,
    val uid: Long,
    val title: String?,
    val content: String,
    val categoryIds: List<Long>,
    val commentCount: Long,
    val likeCount: Long,
    val viewCount: Long,
    val createdAt: LocalDateTime,
    val modifiedAt: LocalDateTime?,
)
