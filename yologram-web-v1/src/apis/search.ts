import api from '../lib/api'
import type { News } from './news'
import type { PostSummary } from './pms'

/** 정렬 기준 — 백엔드 sort 파라미터와 같은 값 */
export type SearchSort = 'RELEVANCE' | 'LATEST'

/** offset 페이지 응답 (검색은 커서가 아니라 페이지 번호를 쓴다 — 총건수·페이지 수가 필요하다) */
export interface SearchPage<T> {
  data: T[]
  page: number
  size: number
  totalPages: number
  totalCount: number
  first: boolean
  last: boolean
}

export type PostSearchPage = SearchPage<PostSummary>
export type NewsSearchPage = SearchPage<News>

export interface SearchParams {
  q: string
  page?: number
  size?: number
  sort?: SearchSort
}

/** 검색 쿼리 문자열 — 지정한 값만 보낸다(미지정은 백엔드 기본값을 쓴다) */
function toQuery(params: SearchParams) {
  return {
    q: params.q,
    ...(params.page != null ? { page: params.page } : {}),
    ...(params.size != null ? { size: params.size } : {}),
    ...(params.sort ? { sort: params.sort } : {}),
  }
}

/**
 * 게시글 검색 — 제목·본문을 형태소(nori) 기준으로 검색한다.
 * 400: 검색어 없음(BLANK_SEARCH_KEYWORD)·조회 한계 초과(SEARCH_PAGE_TOO_DEEP)
 * 503: 검색 설정 없음(SEARCH_UNAVAILABLE)
 */
export async function searchPosts(
  section: string,
  params: SearchParams
): Promise<PostSearchPage> {
  const response = await api.get<PostSearchPage>(`/api/v1/search/${section}/posts`, {
    params: toQuery(params),
  })
  return response.data
}

/**
 * 뉴스 검색 — 제목·요약을 형태소(nori) 기준으로 검색한다.
 * 응답 스키마는 뉴스 목록 API와 같아 같은 카드(NewsCard)를 쓴다.
 * 게시글 검색과 달리 인증이 없다(개인화 값이 없다).
 */
export async function searchNews(
  section: string,
  params: SearchParams
): Promise<NewsSearchPage> {
  const response = await api.get<NewsSearchPage>(`/api/v1/search/${section}/news`, {
    params: toQuery(params),
  })
  return response.data
}
