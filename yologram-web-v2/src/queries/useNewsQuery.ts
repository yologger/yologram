'use client'

import { useInfiniteQuery } from '@tanstack/react-query'
import { getNews } from '@/apis/news'

const PAGE_SIZE = 20

export default function useNewsQuery(categoryId: number | null) {
  return useInfiniteQuery({
    queryKey: ['news', 'tech', categoryId],
    queryFn: ({ pageParam }) =>
      getNews({ categoryId, cursor: pageParam, size: PAGE_SIZE }),
    initialPageParam: undefined as string | null | undefined,
    // nextCursor 유무로 다음 페이지 판단 (마지막 페이지는 필드 생략)
    getNextPageParam: (lastPage) => lastPage.nextCursor ?? undefined,
    select: (data) => data.pages.flatMap((page) => page.data),
  })
}
