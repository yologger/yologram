import { useInfiniteQuery } from '@tanstack/react-query'
import { getComments, type CommentSort } from '../apis/comments'

const PAGE_SIZE = 20

// sort: latest(최신 위) | oldest(오래된 위). 커서 기반 무한스크롤 (내 글 목록과 동일 패턴)
export default function useCommentsQuery(postId: number, sort: CommentSort) {
  return useInfiniteQuery({
    queryKey: ['comments', postId, sort],
    queryFn: ({ pageParam }) =>
      getComments(postId, { sort, cursor: pageParam, size: PAGE_SIZE }),
    initialPageParam: undefined as string | null | undefined,
    getNextPageParam: (lastPage) => lastPage.nextCursor ?? undefined,
    select: (data) => data.pages.flatMap((page) => page.data),
    enabled: Number.isFinite(postId),
  })
}
