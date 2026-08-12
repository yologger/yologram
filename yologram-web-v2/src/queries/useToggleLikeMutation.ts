'use client'

import { useMutation, useQueryClient, type InfiniteData, type QueryKey } from '@tanstack/react-query'
import { likePost, unlikePost, type PostDetail, type PostPage, type PostSummary } from '@/apis/pms'

interface ToggleLikeVariables {
  // 소문자 섹션 (tech 등) — API 경로·쿼리 키에 그대로 사용
  section: string
  id: number
  // true = 좋아요 등록(POST), false = 취소(DELETE)
  like: boolean
}

// 좋아요 토글 — 클릭 즉시 상세·피드·내 글 캐시의 likedByMe/likeCount를 반영(옵티미스틱)하고,
// 실패 시 스냅샷으로 원복. 에러 토스트는 호출부 onError에서 처리
export default function useToggleLikeMutation() {
  const queryClient = useQueryClient()

  return useMutation({
    mutationFn: ({ section, id, like }: ToggleLikeVariables) =>
      like ? likePost(section, id) : unlikePost(section, id),
    onMutate: async ({ section, id, like }) => {
      // 진행 중인 재조회가 옵티미스틱 값을 덮어쓰지 않도록 취소
      await Promise.all([
        queryClient.cancelQueries({ queryKey: ['post', section, id] }),
        queryClient.cancelQueries({ queryKey: ['posts', section] }),
        queryClient.cancelQueries({ queryKey: ['my-posts'] }),
      ])

      // 실패 시 원복용 스냅샷 (카테고리 필터 등 하위 키 전부 포함)
      const snapshots: Array<[QueryKey, unknown]> = [
        ...queryClient.getQueriesData({ queryKey: ['post', section, id] }),
        ...queryClient.getQueriesData({ queryKey: ['posts', section] }),
        ...queryClient.getQueriesData({ queryKey: ['my-posts'] }),
      ]

      // metrics만 교체 — likeCount는 음수가 되지 않게 방어
      const applyMetrics = <P extends PostSummary | PostDetail>(post: P): P => ({
        ...post,
        metrics: {
          ...post.metrics,
          likedByMe: like,
          likeCount: Math.max(0, post.metrics.likeCount + (like ? 1 : -1)),
        },
      })

      // 상세 캐시
      queryClient.setQueryData<PostDetail>(['post', section, id], (old) =>
        old ? applyMetrics(old) : old,
      )

      // 무한스크롤 목록 캐시(피드·내 글) — 페이지 내 해당 글만 교체
      // 내 글 목록은 여러 섹션이 섞여 있어 id + 섹션(대소문자 무시)으로 매칭
      const updatePages = (old: InfiniteData<PostPage> | undefined) => {
        if (!old) return old
        return {
          ...old,
          pages: old.pages.map((page) => ({
            ...page,
            data: page.data.map((p) =>
              p.id === id && p.section.toLowerCase() === section ? applyMetrics(p) : p,
            ),
          })),
        }
      }
      queryClient.setQueriesData<InfiniteData<PostPage>>({ queryKey: ['posts', section] }, updatePages)
      queryClient.setQueriesData<InfiniteData<PostPage>>({ queryKey: ['my-posts'] }, updatePages)

      return { snapshots }
    },
    onError: (_error, _variables, context) => {
      // 실패 시 옵티미스틱 반영 전 상태로 원복
      context?.snapshots.forEach(([queryKey, data]) => {
        queryClient.setQueryData(queryKey, data)
      })
    },
  })
}
