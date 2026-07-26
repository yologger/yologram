'use client'

import ReactMarkdown from 'react-markdown'
import type { Article } from '@/apis/articles'
import { formatRelativeTime } from '@/lib/date'
import styles from './ArticleCard.module.css'

interface Props {
  article: Article
}

export default function ArticleCard({ article }: Props) {
  return (
    <div className={styles.card}>
      <div className={styles.head}>
        <span className={styles.source}>{article.sourceName}</span>
        <span className={styles.dot}>·</span>
        <span className={styles.time}>{formatRelativeTime(article.publishedAt)}</span>
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
      {article.categories.length > 0 && (
        <div className={styles.badges}>
          {article.categories.map((c) => (
            <span key={c} className={styles.badge}>{c}</span>
          ))}
        </div>
      )}
    </div>
  )
}
