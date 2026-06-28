import { useInfiniteQuery } from '@tanstack/react-query'
import { getMyPosts } from '../apis/pms'

const PAGE_SIZE = 20

// section 생략(null) 시 전체 섹션. 값은 tech/invest/politics 소문자
export default function useMyPostsQuery(section: string | null) {
  return useInfiniteQuery({
    queryKey: ['my-posts', section],
    queryFn: ({ pageParam }) =>
      getMyPosts({ section, cursor: pageParam, size: PAGE_SIZE }),
    initialPageParam: undefined as string | null | undefined,
    // nextCursor 유무로 다음 페이지 판단 (피드와 동일)
    getNextPageParam: (lastPage) => lastPage.nextCursor ?? undefined,
    select: (data) => data.pages.flatMap((page) => page.data),
  })
}
