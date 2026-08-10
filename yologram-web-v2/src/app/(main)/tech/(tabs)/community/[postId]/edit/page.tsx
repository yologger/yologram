'use client'

import { useEffect } from 'react'
import { useParams, useRouter } from 'next/navigation'
import { useAtomValue } from 'jotai'
import { useQueryClient } from '@tanstack/react-query'
import { ArrowLeftOutlined } from '@ant-design/icons'
import { App } from 'antd'
import { authAtom } from '@/stores/auth'
import CommunityPostForm from '@/components/community/CommunityPostForm'
import usePostQuery from '@/queries/usePostQuery'
import useUpdatePostMutation from '@/queries/useUpdatePostMutation'
import RequireAuth from '@/components/auth/RequireAuth'
import { getErrorStatus } from '@/lib/error'
import styles from '../CommunityDetail.module.css'

function EditPageInner() {
  const params = useParams<{ postId: string }>()
  const router = useRouter()
  const { message } = App.useApp()
  const queryClient = useQueryClient()
  const id = Number(params.postId)
  const auth = useAtomValue(authAtom)

  const { data: post, isLoading, isError, error } = usePostQuery('tech', id)
  const { mutate: updatePost, isPending } = useUpdatePostMutation()

  const goDetail = () => router.push(`/tech/community/${id}`)

  // 본인 글이 아니면 상세로 되돌림 (서버도 403으로 막지만 UI 선차단)
  const isOwner = !!post && !!auth && post.author.uid === auth.uid
  useEffect(() => {
    if (post && auth && !isOwner) {
      message.error('본인 글만 수정할 수 있어요.')
      router.replace(`/tech/community/${id}`)
    }
  }, [post, auth, isOwner, id, router, message])

  if (isLoading) {
    return (
      <div className={styles.container}>
        <div className={styles.header}>
          <button className={styles.back} aria-label="뒤로" onClick={goDetail}>
            <ArrowLeftOutlined />
          </button>
        </div>
        <div style={{ padding: 16 }}>불러오는 중…</div>
      </div>
    )
  }

  if ((isError && getErrorStatus(error) === 404) || !post) {
    return (
      <div className={styles.container}>
        <div className={styles.header}>
          <button className={styles.back} aria-label="뒤로" onClick={goDetail}>
            <ArrowLeftOutlined />
          </button>
        </div>
        <div style={{ padding: 16 }}>존재하지 않는 글입니다.</div>
      </div>
    )
  }

  if (!isOwner) return null

  const handleSubmit = (values: { title?: string; content: string; categoryIds: number[] }) => {
    updatePost(
      { section: 'tech', id, request: { title: values.title ?? null, content: values.content, categoryIds: values.categoryIds } },
      {
        onSuccess: () => {
          // 상세/목록 재조회로 수정 내용 반영
          queryClient.invalidateQueries({ queryKey: ['post', 'tech', id] })
          queryClient.invalidateQueries({ queryKey: ['posts', 'tech'] })
          queryClient.invalidateQueries({ queryKey: ['my-posts'] })
          message.success('글이 수정되었습니다.')
          router.push(`/tech/community/${id}`)
        },
        onError: () => {
          message.error('글 수정에 실패했어요. 잠시 후 다시 시도해주세요.')
        },
      },
    )
  }

  return (
    <CommunityPostForm
      section="tech"
      submitLabel="수정"
      initialValues={{ title: post.title ?? '', content: post.content, categoryIds: post.categoryIds }}
      isSubmitting={isPending}
      onCancel={goDetail}
      onSubmit={handleSubmit}
    />
  )
}

export default function CommunityEdit() {
  return (
    <RequireAuth>
      <EditPageInner />
    </RequireAuth>
  )
}
