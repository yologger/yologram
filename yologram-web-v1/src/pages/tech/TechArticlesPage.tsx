import { useEffect, useRef, useState } from 'react'
import { Tag } from 'antd'
import ReactMarkdown from 'react-markdown'
import FilterChips, { type ChipItem } from '../../components/common/FilterChips'
import ScrollToTopButton from '../../components/common/ScrollToTopButton'
import useArticlesQuery from '../../queries/useArticlesQuery'
import usePostCategoriesQuery from '../../queries/usePostCategoriesQuery'
import { formatRelativeTime } from '../../lib/date'
import type { Article } from '../../apis/articles'
import styles from './TechArticlesPage.module.css'

function ArticleCard({ article }: { article: Article }) {
  return (
    <div className={styles.card}>
      <div className={styles.head}>
        <span className={styles.source}>{article.sourceName}</span>
        <span className={styles.dot}>·</span>
        <span className={styles.time}>{formatRelativeTime(article.publishedAt)}</span>
        <span className={styles.tags}>
          {article.categories.map((c) => (
            <Tag key={c} color="cyan">{c}</Tag>
          ))}
        </span>
      </div>
      <a
        className={styles.title}
        href={article.link}
        target="_blank"
        rel="noopener noreferrer"
      >
        {article.title}
      </a>
      <div className={styles.summary}>
        <ReactMarkdown>{article.summary}</ReactMarkdown>
      </div>
    </div>
  )
}

export default function TechArticlesPage() {
  // 칩 어휘는 게시판·아티클 공용 카테고리 API가 단일 소스 (커뮤니티 피드와 동일)
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
