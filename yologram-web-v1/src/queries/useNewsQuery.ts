import { useInfiniteQuery } from '@tanstack/react-query'
import { getArticles } from '../apis/articles'

const PAGE_SIZE = 20

// 테크 아티클 커서 기반 무한스크롤 (커뮤니티 피드와 동일 패턴)
export default function useArticlesQuery(categoryId: number | null) {
  return useInfiniteQuery({
    queryKey: ['articles', 'tech', categoryId],
    queryFn: ({ pageParam }) =>
      getArticles({ categoryId, cursor: pageParam, size: PAGE_SIZE }),
    initialPageParam: undefined as string | null | undefined,
    // 마지막 페이지는 nextCursor가 생략되므로 undefined → 더 없음
    getNextPageParam: (lastPage) => lastPage.nextCursor ?? undefined,
    select: (data) => data.pages.flatMap((page) => page.data),
  })
}
