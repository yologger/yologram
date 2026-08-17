import { useQuery } from '@tanstack/react-query'
import { searchNews, type SearchSort } from '@/apis/search'
import { SEARCH_PAGE_SIZE } from './usePostSearchQuery'

/**
 * 뉴스 검색 — 게시글 검색(usePostSearchQuery)과 같은 구조.
 * 페이지 단위 조회이고 placeholderData로 페이지 전환 중 이전 결과를 유지한다.
 */
export default function useNewsSearchQuery(
  section: string,
  keyword: string,
  page: number,
  sort: SearchSort
) {
  return useQuery({
    queryKey: ['news-search', section, keyword, page, sort],
    queryFn: () => searchNews(section, { q: keyword, page, size: SEARCH_PAGE_SIZE, sort }),
    // 검색어가 없으면 요청하지 않는다 (백엔드도 400을 준다)
    enabled: keyword.trim().length > 0,
    placeholderData: (previous) => previous,
  })
}
