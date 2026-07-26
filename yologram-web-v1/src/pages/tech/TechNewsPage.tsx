import { useEffect, useRef, useState } from 'react'
import { Tag } from 'antd'
import ReactMarkdown from 'react-markdown'
import FilterChips, { type ChipItem } from '../../components/common/FilterChips'
import ScrollToTopButton from '../../components/common/ScrollToTopButton'
import useNewsQuery from '../../queries/useNewsQuery'
import usePostCategoriesQuery from '../../queries/usePostCategoriesQuery'
import { formatRelativeTime } from '../../lib/date'
import type { News } from '../../apis/news'
import styles from './TechNewsPage.module.css'

function NewsCard({ news }: { news: News }) {
  return (
    <div className={styles.card}>
      <div className={styles.head}>
        <span className={styles.source}>{news.sourceName}</span>
        <span className={styles.dot}>·</span>
        <span className={styles.time}>{formatRelativeTime(news.publishedAt)}</span>
        <span className={styles.tags}>
          {news.categories.map((c) => (
            <Tag key={c} color="cyan">{c}</Tag>
          ))}
        </span>
      </div>
      <a
        className={styles.title}
        href={news.link}
        target="_blank"
        rel="noopener noreferrer"
      >
        {news.title}
      </a>
      <div className={styles.summary}>
        <ReactMarkdown>{news.summary}</ReactMarkdown>
      </div>
    </div>
  )
}

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
