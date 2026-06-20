import { useInfiniteQuery } from '@tanstack/react-query'
import { getPosts } from '../apis/pms'

const PAGE_SIZE = 15

export default function usePostsQuery(section: string, categoryId: number | null) {
  return useInfiniteQuery({
    queryKey: ['posts', section, categoryId],
    queryFn: ({ pageParam }) =>
      getPosts(section, { cursor: pageParam, size: PAGE_SIZE, categoryId }),
    initialPageParam: undefined as string | null | undefined,
    // nextCursor 유무로 다음 페이지 판단 (legacy 웹과 동일)
    getNextPageParam: (lastPage) => lastPage.nextCursor ?? undefined,
    select: (data) => data.pages.flatMap((page) => page.data),
  })
}
