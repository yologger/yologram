import { useEffect, useRef, useState } from 'react'
import { useNavigate } from 'react-router'
import { useAtomValue } from 'jotai'
import { techPostsAtom } from '../../stores/techCommunity'
import PostCard from './community/PostCard'
import ScrollToTopButton from '../../components/common/ScrollToTopButton'
import FilterChips from '../../components/common/FilterChips'
import { TECH_FILTER_CATEGORIES, ALL_CATEGORY } from '../../constants/techCategories'
import styles from './community/TechCommunity.module.css'

const PAGE_SIZE = 15

export default function TechCommunityPage() {
  const navigate = useNavigate()
  const posts = useAtomValue(techPostsAtom)
  const [filter, setFilter] = useState(ALL_CATEGORY)
  const [visibleCount, setVisibleCount] = useState(PAGE_SIZE)
  const sentinelRef = useRef<HTMLDivElement>(null)

  const filteredPosts = filter === ALL_CATEGORY
    ? posts
    : posts.filter((p) => p.categories.includes(filter))

  const hasMore = visibleCount < filteredPosts.length

  const handleFilterChange = (item: string) => {
    setFilter(item)
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
      <FilterChips items={TECH_FILTER_CATEGORIES} selected={filter} onChange={handleFilterChange} />
      {filteredPosts.slice(0, visibleCount).map((post) => (
        <PostCard key={post.id} post={post} onClick={() => navigate(`/tech/community/${post.id}`)} />
      ))}
      {hasMore && <div ref={sentinelRef} className={styles.sentinel} />}

      <div className={styles.composeBar}>
        <button className={styles.composeInput} onClick={() => navigate('/tech/community/write')}>
          기술 커뮤니티에 글을 남겨보세요
        </button>
      </div>

      <ScrollToTopButton />
    </div>
  )
}
