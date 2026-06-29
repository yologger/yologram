'use client'

import { Avatar } from 'antd'
import { UserOutlined, HeartOutlined, MessageOutlined, DeleteOutlined } from '@ant-design/icons'
import type { PostSummary } from '@/apis/pms'
import { formatRelativeTime } from '@/lib/date'
import styles from './PostCard.module.css'

interface Props {
  post: PostSummary
  categoryNames?: string[]
  onClick?: () => void
  onDelete?: () => void
}

export default function PostCard({ post, categoryNames = [], onClick, onDelete }: Props) {
  return (
    <div className={styles.card} onClick={onClick}>
      <div className={styles.head}>
        <Avatar size={32} icon={<UserOutlined />} />
        <span className={styles.author}>{post.author.nickname ?? '알 수 없음'}</span>
        <span className={styles.time}>{formatRelativeTime(post.createdAt)}</span>
        {onDelete && (
          <button
            type="button"
            className={styles.delete}
            aria-label="삭제"
            onClick={(e) => {
              e.stopPropagation()
              onDelete()
            }}
          >
            <DeleteOutlined />
          </button>
        )}
      </div>
      {post.title && <div className={styles.title}>{post.title}</div>}
      <div className={styles.content}>{post.content}</div>
      {categoryNames.length > 0 && (
        <div className={styles.badges}>
          {categoryNames.map((c) => (
            <span key={c} className={styles.badge}>{c}</span>
          ))}
        </div>
      )}
      <div className={styles.meta}>
        <span><HeartOutlined /> {post.likeCount}</span>
        <span><MessageOutlined /> {post.commentCount}</span>
      </div>
    </div>
  )
}
