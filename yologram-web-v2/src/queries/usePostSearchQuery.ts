import { useQuery } from '@tanstack/react-query'
import { searchPosts, type SearchSort } from '@/apis/search'

/** 한 페이지 크기 — 백엔드 상한은 50 */
export const SEARCH_PAGE_SIZE = 10

/**
 * 게시글 검색 — 페이지 단위 조회(useInfiniteQuery가 아니라 useQuery).
 * 검색은 페이지 네비게이션을 쓰므로 이전 페이지를 누적하지 않는다.
 * placeholderData로 페이지 전환 중 이전 결과를 유지해 목록이 깜빡이지 않게 한다.
 */
export default function usePostSearchQuery(
  section: string,
  keyword: string,
  page: number,
  sort: SearchSort
) {
  return useQuery({
    queryKey: ['post-search', section, keyword, page, sort],
    queryFn: () => searchPosts(section, { q: keyword, page, size: SEARCH_PAGE_SIZE, sort }),
    // 검색어가 없으면 요청하지 않는다 (백엔드도 400을 준다)
    enabled: keyword.trim().length > 0,
    placeholderData: (previous) => previous,
  })
}
