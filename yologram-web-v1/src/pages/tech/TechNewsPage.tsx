import { useEffect, useRef, useState } from 'react'
import FilterChips, { type ChipItem } from '../../components/common/FilterChips'
import NewsCard from '../../components/news/NewsCard'
import ScrollToTopButton from '../../components/common/ScrollToTopButton'
import useNewsQuery from '../../queries/useNewsQuery'
import usePostCategoriesQuery from '../../queries/usePostCategoriesQuery'
import styles from './TechNewsPage.module.css'

export default function TechNewsPage() {
  // 칩 어휘는 게시판·뉴스 공용 카테고리 API가 단일 소스 (커뮤니티 피드와 동일)
  const { data: categories = [] } = usePostCategoriesQuery('tech')
  const [categoryId, setCategoryId] = useState<number | null>(null)
  const sentinelRef = useRef<HTMLDivElement>(null)

  const {
    data: newsList = [],
    fetchNextPage,
    hasNextPage,
    isFetchingNextPage,
    isLoading,
    isError,
    refetch,
  } = useNewsQuery(categoryId)

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
      <FilterChips items={filterItems} selected={categoryId} onChange={setCategoryId} />

      {isLoading && <div className={styles.status}>불러오는 중…</div>}

      {isError && (
        <div className={styles.status}>
          <p>뉴스를 불러오지 못했어요. 잠시 후 다시 시도해주세요.</p>
          <button onClick={() => refetch()}>다시 시도</button>
        </div>
      )}

      {!isLoading && !isError && newsList.length === 0 && (
        <div className={styles.status}>아직 뉴스가 없어요.</div>
      )}

      {newsList.map((news) => (
        <NewsCard key={news.id} news={news} />
      ))}
      {hasNextPage && <div ref={sentinelRef} className={styles.sentinel} />}

      <ScrollToTopButton />
    </div>
  )
}
