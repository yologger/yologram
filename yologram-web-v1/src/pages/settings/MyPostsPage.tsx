import { useEffect, useMemo, useRef, useState } from 'react'
import { useNavigate } from 'react-router'
import { Typography } from 'antd'
import { ArrowLeftOutlined } from '@ant-design/icons'
import FilterChips, { type ChipItem } from '../../components/common/FilterChips'
import PostCard from '../tech/community/PostCard'
import usePostCategoriesQuery from '../../queries/usePostCategoriesQuery'
import useMyPostsQuery from '../../queries/useMyPostsQuery'
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
          <PostCard
            key={post.id}
            post={post}
            categoryNames={post.categoryIds.map((id) => nameById.get(id)).filter((n): n is string => !!n)}
            onClick={post.section === 'TECH' ? () => navigate(`/tech/community/${post.id}`) : undefined}
          />
        ))}
        {hasNextPage && <div ref={sentinelRef} className={styles.sentinel} />}
      </div>
    </div>
  )
}
