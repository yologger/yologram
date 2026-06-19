'use client'

import { useEffect, useMemo, useRef, useState } from 'react'
import { useRouter } from 'next/navigation'
import { useAtomValue } from 'jotai'
import { communityPostsAtom } from '@/stores/community'
import PostCard from '@/components/community/PostCard'
import ScrollToTopButton from '@/components/common/ScrollToTopButton'
import FilterChips, { type ChipItem } from '@/components/common/FilterChips'
import useCategoriesQuery from '@/queries/useCategoriesQuery'
import styles from './TechCommunity.module.css'

const PAGE_SIZE = 15

export default function TechCommunity() {
  const router = useRouter()
  const posts = useAtomValue(communityPostsAtom)
  const { data: categories = [] } = useCategoriesQuery('tech')
  const [filter, setFilter] = useState<number | null>(null)
  const [visibleCount, setVisibleCount] = useState(PAGE_SIZE)
  const sentinelRef = useRef<HTMLDivElement>(null)

  const nameById = useMemo(() => new Map(categories.map((c) => [c.id, c.name])), [categories])

  const filterItems: Array<ChipItem<number | null>> = [
    { label: '전체', value: null },
    ...categories.map((c) => ({ label: c.name, value: c.id as number | null })),
  ]

  const filteredPosts = filter === null
    ? posts
    : posts.filter((p) => p.categoryIds.includes(filter))

  const hasMore = visibleCount < filteredPosts.length

  const handleFilterChange = (value: number | null) => {
    setFilter(value)
    setVisibleCount(PAGE_SIZE)
  }

  useEffect(() => {
    if (!hasMore) return
    const el = sentinelRef.current
    if (!el) return

    const observer = new IntersectionObserver((entries) => {
      if (entries[0].isIntersecting) {
        setVisibleCount((c) => Math.min(c + PAGE_SIZE, filteredPosts.length))
      }
    })
    observer.observe(el)
    return () => observer.disconnect()
  }, [hasMore, filteredPosts.length])

  return (
    <div className={styles.feed}>
      <FilterChips items={filterItems} selected={filter} onChange={handleFilterChange} />
      {filteredPosts.slice(0, visibleCount).map((post) => (
        <PostCard
          key={post.id}
          post={post}
          categoryNames={post.categoryIds.map((id) => nameById.get(id)).filter((n): n is string => !!n)}
          onClick={() => router.push(`/tech/community/${post.id}`)}
        />
      ))}
      {hasMore && <div ref={sentinelRef} className={styles.sentinel} />}

      <div className={styles.composeBar}>
        <button className={styles.composeInput} onClick={() => router.push('/tech/community/write')}>
          기술 커뮤니티에 글을 남겨보세요
        </button>
      </div>

      <ScrollToTopButton />
    </div>
  )
}
