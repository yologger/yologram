import { useEffect, useMemo, useRef, useState } from 'react'
import { useNavigate } from 'react-router'
import PostCard from './community/PostCard'
import ScrollToTopButton from '../../components/common/ScrollToTopButton'
import FilterChips, { type ChipItem } from '../../components/common/FilterChips'
import usePostCategoriesQuery from '../../queries/usePostCategoriesQuery'
import usePostsQuery from '../../queries/usePostsQuery'
import styles from './community/TechCommunity.module.css'

export default function TechCommunityPage() {
  const navigate = useNavigate()
  const { data: categories = [] } = usePostCategoriesQuery('tech')
  const [filter, setFilter] = useState<number | null>(null)
  const sentinelRef = useRef<HTMLDivElement>(null)

  const {
    data: posts = [],
    fetchNextPage,
    hasNextPage,
    isFetchingNextPage,
    isLoading,
    isError,
    refetch,
  } = usePostsQuery('tech', filter)

  const nameById = useMemo(() => new Map(categories.map((c) => [c.id, c.name])), [categories])

  const filterItems: Array<ChipItem<number | null>> = [
    { label: '전체', value: null },
    ...categories.map((c) => ({ label: c.name, value: c.id as number | null })),
  ]

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
    <div className={styles.feed}>
      <FilterChips items={filterItems} selected={filter} onChange={setFilter} />

      {isLoading && <div className={styles.status}>불러오는 중…</div>}

      {isError && (
        <div className={styles.status}>
          <p>글을 불러오지 못했어요. 잠시 후 다시 시도해주세요.</p>
          <button onClick={() => refetch()}>다시 시도</button>
        </div>
      )}

      {!isLoading && !isError && posts.length === 0 && (
        <div className={styles.status}>아직 게시글이 없어요.</div>
      )}

      {posts.map((post) => (
        <PostCard
          key={post.id}
          post={post}
          categoryNames={post.categoryIds.map((id) => nameById.get(id)).filter((n): n is string => !!n)}
          onClick={() => navigate(`/tech/community/${post.id}`)}
        />
      ))}
      {hasNextPage && <div ref={sentinelRef} className={styles.sentinel} />}

      <div className={styles.composeBar}>
        <button className={styles.composeInput} onClick={() => navigate('/tech/community/write')}>
          기술 커뮤니티에 글을 남겨보세요
        </button>
      </div>

      <ScrollToTopButton />
    </div>
  )
}
