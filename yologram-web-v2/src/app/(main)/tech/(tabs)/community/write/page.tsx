'use client'

import { useRouter } from 'next/navigation'
import { useQueryClient } from '@tanstack/react-query'
import { App } from 'antd'
import CommunityPostForm from '@/components/community/CommunityPostForm'
import useCreatePostMutation from '@/queries/useCreatePostMutation'
import RequireAuth from '@/components/auth/RequireAuth'

export default function CommunityWrite() {
  const router = useRouter()
  const { message } = App.useApp()
  const queryClient = useQueryClient()
  const { mutate: createPost, isPending } = useCreatePostMutation()

  const handleSubmit = (values: { title?: string; content: string; categoryIds: number[] }) => {
    createPost(
      { section: 'tech', request: values },
      {
        onSuccess: () => {
          // 피드 목록 무효화 → 최신순 재조회 시 새 글이 맨 위에 노출
          queryClient.invalidateQueries({ queryKey: ['posts', 'tech'] })
          message.success('글이 등록되었습니다.')
          router.push('/tech/community')
        },
        onError: () => {
          message.error('글 등록에 실패했어요. 잠시 후 다시 시도해주세요.')
        },
      },
    )
  }

  return (
    <RequireAuth>
      <CommunityPostForm
        section="tech"
        submitLabel="남기기"
        isSubmitting={isPending}
        onCancel={() => router.push('/tech/community')}
        onSubmit={handleSubmit}
      />
    </RequireAuth>
  )
}
