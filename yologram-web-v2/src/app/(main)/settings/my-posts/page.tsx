'use client'

import { useEffect, useMemo, useRef, useState } from 'react'
import { useRouter } from 'next/navigation'
import { useQueryClient } from '@tanstack/react-query'
import { App, Typography } from 'antd'
import { ArrowLeftOutlined } from '@ant-design/icons'
import FilterChips, { type ChipItem } from '@/components/common/FilterChips'
import PostCard from '@/components/community/PostCard'
import RequireAuth from '@/components/auth/RequireAuth'
import type { PostSummary } from '@/apis/pms'
import usePostCategoriesQuery from '@/queries/usePostCategoriesQuery'
import useMyPostsQuery from '@/queries/useMyPostsQuery'
import useDeletePostMutation from '@/queries/useDeletePostMutation'
import styles from './MyPosts.module.css'

const { Title } = Typography

// 값은 백엔드 section 파라미터(소문자). null = 전체(파라미터 생략)
const SECTION_TABS: Array<ChipItem<string | null>> = [
  { label: '전체', value: null },
  { label: '기술', value: 'tech' },
  { label: '투자', value: 'invest' },
  { label: '정치', value: 'politics' },
]

export default function MyPosts() {
  const router = useRouter()
  const { modal, message } = App.useApp()
  const queryClient = useQueryClient()
  const { mutate: deletePost } = useDeletePostMutation()
  const [section, setSection] = useState<string | null>(null)
  const sentinelRef = useRef<HTMLDivElement>(null)

  const {
    data: posts = [],
    fetchNextPage,
    hasNextPage,
    isFetchingNextPage,
    isLoading,
    isError,
    refetch,
  } = useMyPostsQuery(section)

  // 글이 여러 섹션에 걸쳐 있을 수 있어 세 섹션 카테고리를 모두 받아 id→name 병합
  const { data: tech = [] } = usePostCategoriesQuery('tech')
  const { data: invest = [] } = usePostCategoriesQuery('invest')
  const { data: politics = [] } = usePostCategoriesQuery('politics')
  const nameById = useMemo(
    () => new Map([...tech, ...invest, ...politics].map((c) => [c.id, c.name])),
    [tech, invest, politics],
  )

  const handleDelete = (post: PostSummary) => {
    const sectionParam = post.section.toLowerCase()
    modal.confirm({
      title: '글을 삭제할까요?',
      content: '삭제한 글은 복구할 수 없어요.',
      okText: '삭제',
      okButtonProps: { danger: true },
      cancelText: '취소',
      onOk: () =>
        new Promise<void>((resolve) => {
          deletePost(
            { section: sectionParam, id: post.id },
            {
              onSuccess: () => {
                // 내 글 목록/피드/상세 재조회로 삭제 반영
                queryClient.invalidateQueries({ queryKey: ['my-posts'] })
                queryClient.invalidateQueries({ queryKey: ['posts', sectionParam] })
                queryClient.removeQueries({ queryKey: ['post', sectionParam, post.id] })
                // 삭제된 글은 다시 볼 일이 없으므로 댓글 캐시도 제거(백엔드도 함께 삭제)
                queryClient.removeQueries({ queryKey: ['comments', sectionParam, post.id] })
                message.success('글이 삭제되었습니다.')
                resolve()
              },
              onError: () => {
                // 실패해도 모달은 닫고 에러 토스트만(reject 시 unhandled rejection 발생)
                message.error('글 삭제에 실패했어요. 잠시 후 다시 시도해주세요.')
                resolve()
              },
            },
          )
        }),
    })
  }

  useEffect(() => {
    if (!hasNextPage) return
    const el = sentinelRef.current
    if (!el) return

    const observer = new IntersectionObserver((entries) => {
      if (entries[0].isIntersecting && !isFetchingNextPage) {
        fetchNextPage()
      }
    })
    observer.observe(el)
    return () => observer.disconnect()
  }, [hasNextPage, isFetchingNextPage, fetchNextPage])

  return (
    <RequireAuth>
      <div className={styles.container}>
        <div className={styles.header}>
          <button className={styles.back} aria-label="뒤로" onClick={() => router.push('/settings')}>
            <ArrowLeftOutlined />
          </button>
          <Title level={4} style={{ margin: 0 }}>내가 쓴 글</Title>
        </div>

        <div className={styles.body}>
          <FilterChips items={SECTION_TABS} selected={section} onChange={setSection} />

          {isLoading && <div className={styles.status}>불러오는 중…</div>}

          {isError && (
            <div className={styles.status}>
              <p>글을 불러오지 못했어요. 잠시 후 다시 시도해주세요.</p>
              <button onClick={() => refetch()}>다시 시도</button>
            </div>
          )}

          {!isLoading && !isError && posts.length === 0 && (
            <div className={styles.empty}>작성한 글이 없어요</div>
          )}

          {posts.map((post) => (
            <PostCard
              key={post.id}
              post={post}
              categoryNames={post.categoryIds.map((id) => nameById.get(id)).filter((n): n is string => !!n)}
              onClick={post.section === 'TECH' ? () => router.push(`/tech/community/${post.id}`) : undefined}
              onDelete={() => handleDelete(post)}
            />
          ))}
          {hasNextPage && <div ref={sentinelRef} className={styles.sentinel} />}
        </div>
      </div>
    </RequireAuth>
  )
}
