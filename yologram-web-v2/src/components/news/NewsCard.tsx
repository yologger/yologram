'use client'

import ReactMarkdown from 'react-markdown'
import type { News } from '@/apis/news'
import { formatRelativeTime } from '@/lib/date'
import styles from './NewsCard.module.css'

interface Props {
  news: News
}

export default function NewsCard({ news }: Props) {
  return (
    <div className={styles.card}>
      <div className={styles.head}>
        <span className={styles.source}>{news.sourceName}</span>
        <span className={styles.dot}>·</span>
        <span className={styles.time}>{formatRelativeTime(news.publishedAt)}</span>
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
      {news.categories.length > 0 && (
        <div className={styles.badges}>
          {news.categories.map((c) => (
            <span key={c} className={styles.badge}>{c}</span>
          ))}
        </div>
      )}
    </div>
  )
}
