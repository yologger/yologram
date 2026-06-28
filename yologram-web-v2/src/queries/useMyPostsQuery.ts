import { useInfiniteQuery } from '@tanstack/react-query'
import { getMyPosts } from '@/apis/pms'

const PAGE_SIZE = 20

// section: null이면 전체, 그 외 소문자(tech/invest/politics)
export default function useMyPostsQuery(section: string | null) {
  return useInfiniteQuery({
    queryKey: ['myPosts', section],
    queryFn: ({ pageParam }) =>
      getMyPosts({ section, cursor: pageParam, size: PAGE_SIZE }),
    initialPageParam: undefined as string | null | undefined,
    // nextCursor 유무로 다음 페이지 판단 (피드와 동일)
    getNextPageParam: (lastPage) => lastPage.nextCursor ?? undefined,
    select: (data) => data.pages.flatMap((page) => page.data),
  })
}
