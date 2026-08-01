package link.yologram.worker.domain.news.tech.enums

/** 테크 뉴스 파이프라인 상태: 수집(COLLECTED) → LLM 요약(SUMMARIZED) / 요약 실패(FAILED, retry_count로 재시도) */
enum class TechNewsStatus {
    COLLECTED,
    SUMMARIZED,
    FAILED,
}
