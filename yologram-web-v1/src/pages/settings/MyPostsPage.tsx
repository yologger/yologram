import { useEffect, useMemo, useRef, useState } from 'react'
import { useNavigate } from 'react-router'
import { useQueryClient } from '@tanstack/react-query'
import { App, Typography } from 'antd'
import { ArrowLeftOutlined, DeleteOutlined } from '@ant-design/icons'
import FilterChips, { type ChipItem } from '../../components/common/FilterChips'
import PostCard from '../tech/community/PostCard'
import usePostCategoriesQuery from '../../queries/usePostCategoriesQuery'
import useMyPostsQuery from '../../queries/useMyPostsQuery'
import useDeletePostMutation from '../../queries/useDeletePostMutation'
import styles from './MyPostsPage.module.css'

const { Title } = Typography

// 값은 백엔드 section 파라미터(소문자). null = 전체(파라미터 생략)
const SECTION_TABS: Array<ChipItem<string | null>> = [
  { label: '전체', value: null },
  { label: '기술', value: 'tech' },
  { label: '투자', value: 'invest' },
  { label: '정치', value: 'politics' },
]

export default function MyPostsPage() {
  const navigate = useNavigate()
  const { message, modal } = App.useApp()
  const queryClient = useQueryClient()
  const [section, setSection] = useState<string | null>(null)
  const sentinelRef = useRef<HTMLDivElement>(null)
  const { mutate: deletePost } = useDeletePostMutation()

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

  // 삭제는 되돌릴 수 없으므로 확인 모달을 거친다. section은 백엔드 소문자 파라미터
  const handleDelete = (postSection: string, id: number) => {
    modal.confirm({
      title: '게시글을 삭제할까요?',
      content: '삭제한 글은 되돌릴 수 없어요.',
      okText: '삭제',
      okButtonProps: { danger: true },
      cancelText: '취소',
      onOk: () =>
        new Promise<void>((resolve) => {
          deletePost(
            { section: postSection.toLowerCase(), id },
            {
              onSuccess: () => {
                // 내 글 목록/피드 무효화 → 목록에서 제거
                queryClient.invalidateQueries({ queryKey: ['my-posts'] })
                queryClient.invalidateQueries({ queryKey: ['posts', postSection.toLowerCase()] })
                // 삭제된 글은 다시 볼 일이 없으므로 댓글 캐시는 제거
                queryClient.removeQueries({ queryKey: ['comments', postSection.toLowerCase(), id] })
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

  return (
    <div className={styles.container}>
      <div className={styles.header}>
        <button className={styles.back} aria-label="뒤로" onClick={() => navigate('/settings')}>
          <ArrowLeftOutlined />
        </button>
        <Title level={4} style={{ margin: 0 }}>내가 쓴 글</Title>
      </div>

      <div className={styles.body}>
        <FilterChips items={SECTION_TABS} selected={section} onChange={setSection} />

        {isLoading && <div className={styles.empty}>불러오는 중…</div>}

        {isError && (
          <div className={styles.empty}>
            <p>글을 불러오지 못했어요. 잠시 후 다시 시도해주세요.</p>
            <button onClick={() => refetch()}>다시 시도</button>
          </div>
        )}

        {!isLoading && !isError && posts.length === 0 && (
          <div className={styles.empty}>작성한 글이 없어요</div>
        )}

        {posts.map((post) => (
          <div key={post.id} className={styles.postItem}>
            <PostCard
              post={post}
              categoryNames={post.categoryIds.map((id) => nameById.get(id)).filter((n): n is string => !!n)}
              onClick={post.section === 'TECH' ? () => navigate(`/tech/community/${post.id}`) : undefined}
            />
            <button
              className={styles.deleteButton}
              aria-label="삭제"
              onClick={() => handleDelete(post.section, post.id)}
            >
              <DeleteOutlined />
            </button>
          </div>
        ))}
        {hasNextPage && <div ref={sentinelRef} className={styles.sentinel} />}
      </div>
    </div>
  )
}
