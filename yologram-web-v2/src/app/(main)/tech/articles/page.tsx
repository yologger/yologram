'use client'

import { useEffect, useRef, useState } from 'react'
import FilterChips, { type ChipItem } from '@/components/common/FilterChips'
import ScrollToTopButton from '@/components/common/ScrollToTopButton'
import ArticleCard from '@/components/articles/ArticleCard'
import usePostCategoriesQuery from '@/queries/usePostCategoriesQuery'
import useArticlesQuery from '@/queries/useArticlesQuery'
import styles from './TechArticles.module.css'

export default function TechArticles() {
  // 카테고리 마스터는 게시판·아티클 공용 (tech 카테고리 API가 단일 소스)
  const { data: categories = [] } = usePostCategoriesQuery('tech')
  const [categoryId, setCategoryId] = useState<number | null>(null)
  const sentinelRef = useRef<HTMLDivElement>(null)

  const {
    data: articles = [],
    fetchNextPage,
    hasNextPage,
    isFetchingNextPage,
    isLoading,
    isError,
    refetch,
  } = useArticlesQuery(categoryId)

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
          <p>아티클을 불러오지 못했어요. 잠시 후 다시 시도해주세요.</p>
          <button onClick={() => refetch()}>다시 시도</button>
        </div>
      )}

      {!isLoading && !isError && articles.length === 0 && (
        <div className={styles.status}>아직 아티클이 없어요.</div>
      )}

      {articles.map((article) => (
        <ArticleCard key={article.id} article={article} />
      ))}
      {hasNextPage && <div ref={sentinelRef} className={styles.sentinel} />}

      <ScrollToTopButton />
    </div>
  )
}
