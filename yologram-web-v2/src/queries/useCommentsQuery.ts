'use client'

import { useInfiniteQuery } from '@tanstack/react-query'
import { getComments, type CommentSort } from '@/apis/pms'

const PAGE_SIZE = 20

export default function useCommentsQuery(section: string, postId: number, sort: CommentSort) {
  return useInfiniteQuery({
    queryKey: ['comments', section, postId, sort],
    queryFn: ({ pageParam }) =>
      getComments(section, postId, { sort, cursor: pageParam, size: PAGE_SIZE }),
    initialPageParam: undefined as string | null | undefined,
    // nextCursor 유무로 다음 페이지 판단 (피드/내 글과 동일)
    getNextPageParam: (lastPage) => lastPage.nextCursor ?? undefined,
    select: (data) => data.pages.flatMap((page) => page.data),
    enabled: Number.isFinite(postId),
  })
}
