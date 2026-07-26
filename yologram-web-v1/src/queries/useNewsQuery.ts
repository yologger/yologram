import { useInfiniteQuery } from '@tanstack/react-query'
import { getNews } from '../apis/news'

const PAGE_SIZE = 20

// 테크 뉴스 커서 기반 무한스크롤 (커뮤니티 피드와 동일 패턴)
export default function useNewsQuery(categoryId: number | null) {
  return useInfiniteQuery({
    queryKey: ['news', 'tech', categoryId],
    queryFn: ({ pageParam }) =>
      getNews({ categoryId, cursor: pageParam, size: PAGE_SIZE }),
    initialPageParam: undefined as string | null | undefined,
    // 마지막 페이지는 nextCursor가 생략되므로 undefined → 더 없음
    getNextPageParam: (lastPage) => lastPage.nextCursor ?? undefined,
    select: (data) => data.pages.flatMap((page) => page.data),
  })
}
