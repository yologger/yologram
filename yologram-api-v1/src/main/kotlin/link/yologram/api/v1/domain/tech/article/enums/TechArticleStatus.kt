package link.yologram.api.v1.domain.tech.article.enums

/** 테크 아티클 파이프라인 상태 (worker가 관리). 공개 조회는 SUMMARIZED만 노출 */
enum class TechArticleStatus {
    COLLECTED,
    SUMMARIZED,
    FAILED,
}
