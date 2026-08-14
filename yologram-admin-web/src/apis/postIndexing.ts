import api from '../lib/api'

/** 인덱싱 대상 섹션 — 경로 세그먼트이자 인덱스 구분 (tech만 백엔드 구현됨) */
export type IndexingSection = 'tech' | 'invest' | 'politics'

/**
 * 어드민 게시글 검색 인덱싱 — 실제 색인은 하지 않고 SQS에 작업만 발행한다.
 * 세 엔드포인트 모두 202로 즉시 응답하고 worker가 비동기로 색인한다.
 */
const basePath = (section: IndexingSection) => `/api/v1/search/admin/${section}/posts/indexing`

/** 전체 인덱싱 — 1 ~ max(id)를 20건 단위로 쪼개 발행 */
export async function indexAllPosts(section: IndexingSection): Promise<void> {
  await api.put(basePath(section))
}

/** 단건 인덱싱 */
export async function indexPost(section: IndexingSection, id: number): Promise<void> {
  await api.put(`${basePath(section)}/${id}`)
}

/** 범위 인덱싱 — from > to 또는 0 이하면 400 INVALID_INDEX_RANGE */
export async function indexPostRange(
  section: IndexingSection,
  from: number,
  to: number
): Promise<void> {
  await api.put(`${basePath(section)}/${from}/${to}`)
}
