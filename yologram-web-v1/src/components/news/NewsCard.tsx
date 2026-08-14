import { Tag } from 'antd'
import ReactMarkdown from 'react-markdown'
import type { News } from '../../apis/news'
import { formatRelativeTime } from '../../lib/date'
import styles from './NewsCard.module.css'

interface Props {
  news: News
}

/** 뉴스 카드 — 뉴스 피드와 검색 결과가 공유한다 (web-v2 components/news/NewsCard와 대응) */
export default function NewsCard({ news }: Props) {
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
