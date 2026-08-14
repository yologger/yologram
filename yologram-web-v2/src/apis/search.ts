import api from '@/lib/api'
import type { PostSummary } from './pms'

/** 정렬 기준 — 백엔드 sort 파라미터와 같은 값 */
export type SearchSort = 'RELEVANCE' | 'LATEST'

/** offset 페이지 응답 (검색은 커서가 아니라 페이지 번호를 쓴다 — 총건수·페이지 수가 필요하다) */
export interface PostSearchPage {
  data: PostSummary[]
  page: number
  size: number
  totalPages: number
  totalCount: number
  first: boolean
  last: boolean
}

export interface SearchPostsParams {
  q: string
  page?: number
  size?: number
  sort?: SearchSort
}

/**
 * 게시글 검색 — 제목·본문을 형태소(nori) 기준으로 검색한다.
 * 400: 검색어 없음(BLANK_SEARCH_KEYWORD)·조회 한계 초과(SEARCH_PAGE_TOO_DEEP)
 * 503: 검색 설정 없음(SEARCH_UNAVAILABLE)
 */
export async function searchPosts(
  section: string,
  params: SearchPostsParams
): Promise<PostSearchPage> {
  const response = await api.get<PostSearchPage>(`/api/v2/search/${section}/posts`, {
    params: {
      q: params.q,
      ...(params.page != null ? { page: params.page } : {}),
      ...(params.size != null ? { size: params.size } : {}),
      ...(params.sort ? { sort: params.sort } : {}),
    },
  })
  return response.data
}
