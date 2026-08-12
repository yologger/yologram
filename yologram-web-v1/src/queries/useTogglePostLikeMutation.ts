import { useMutation, useQueryClient, type InfiniteData, type QueryKey } from '@tanstack/react-query'
import { likePost, unlikePost, type PostDetail, type PostPage, type PostSummary } from '../apis/pms'

interface ToggleLikeVariables {
  section: string
  id: number
  // true면 좋아요 등록(POST), false면 취소(DELETE)
  like: boolean
}

// 좋아요 토글 뮤테이션 — 상세(['post'])·피드(['posts'])·내 글(['my-posts']) 캐시를
// 클릭 즉시 옵티미스틱 갱신하고, 실패 시 스냅샷으로 원복한다 (토스트는 호출부 담당).
// 서버 API가 멱등이라 성공 시 서버 상태와 일치하므로 성공 후 재조회(invalidate)는 생략.
export default function useTogglePostLikeMutation() {
  const queryClient = useQueryClient()

  return useMutation({
    mutationFn: ({ section, id, like }: ToggleLikeVariables) =>
      like ? likePost(section, id) : unlikePost(section, id),
    onMutate: async ({ section, id, like }) => {
      // 진행 중인 조회가 옵티미스틱 반영을 덮어쓰지 않도록 취소
      await Promise.all([
        queryClient.cancelQueries({ queryKey: ['post', section, id] }),
        queryClient.cancelQueries({ queryKey: ['posts', section] }),
        queryClient.cancelQueries({ queryKey: ['my-posts'] }),
      ])

      // 실패 시 원복할 스냅샷 (카테고리/섹션 필터 변형 캐시 전부 포함)
      const snapshots: Array<[QueryKey, unknown]> = [
        ...queryClient.getQueriesData({ queryKey: ['post', section, id] }),
        ...queryClient.getQueriesData({ queryKey: ['posts', section] }),
        ...queryClient.getQueriesData({ queryKey: ['my-posts'] }),
      ]

      // 대상 게시글의 metrics만 갱신 (카운트는 0 미만 방지)
      const applyMetrics = <T extends PostSummary | PostDetail>(post: T): T =>
        post.id === id
          ? {
              ...post,
              metrics: {
                ...post.metrics,
                likedByMe: like,
                likeCount: Math.max(0, post.metrics.likeCount + (like ? 1 : -1)),
              },
            }
          : post

      // 상세 캐시
      queryClient.setQueriesData<PostDetail>(
        { queryKey: ['post', section, id] },
        (old) => (old ? applyMetrics(old) : old),
      )

      // 무한스크롤 목록 캐시 (피드·내 글)
      const applyToPages = (old: InfiniteData<PostPage> | undefined) =>
        old
          ? { ...old, pages: old.pages.map((page) => ({ ...page, data: page.data.map(applyMetrics) })) }
          : old
      queryClient.setQueriesData<InfiniteData<PostPage>>({ queryKey: ['posts', section] }, applyToPages)
      queryClient.setQueriesData<InfiniteData<PostPage>>({ queryKey: ['my-posts'] }, applyToPages)

      return { snapshots }
    },
    onError: (_error, _variables, context) => {
      // 실패 시 스냅샷 원복 — 에러 토스트는 호출부(onError)에서 표시
      context?.snapshots.forEach(([queryKey, data]) => queryClient.setQueryData(queryKey, data))
    },
  })
}
