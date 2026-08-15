import api from '../lib/api'

/** 인덱싱 대상 섹션 — 경로 세그먼트이자 인덱스 구분 (tech만 백엔드 구현됨) */
export type IndexingSection = 'tech' | 'invest' | 'politics'

/** 인덱싱 대상 — 게시글·뉴스가 같은 조작을 공유한다 (백엔드도 한 큐에 target으로 구분) */
export type IndexingTarget = 'posts' | 'news'

/**
 * 어드민 검색 인덱싱 — 실제 색인은 하지 않고 SQS에 작업만 발행한다.
 * 세 엔드포인트 모두 202로 즉시 응답하고 worker가 비동기로 색인한다.
 */
const basePath = (section: IndexingSection, target: IndexingTarget) =>
  `/api/v1/search/admin/${section}/${target}/indexing`

/** 전체 인덱싱 — 1 ~ max(id)를 20건 단위로 쪼개 발행 */
export async function indexAll(section: IndexingSection, target: IndexingTarget): Promise<void> {
  await api.put(basePath(section, target))
}

/** 단건 인덱싱 */
export async function indexOne(
  section: IndexingSection,
  target: IndexingTarget,
  id: number
): Promise<void> {
  await api.put(`${basePath(section, target)}/${id}`)
}

/** 범위 인덱싱 — from > to 또는 0 이하면 400 INVALID_INDEX_RANGE */
export async function indexRange(
  section: IndexingSection,
  target: IndexingTarget,
  from: number,
  to: number
): Promise<void> {
  await api.put(`${basePath(section, target)}/${from}/${to}`)
}
