'use client'

import { App, Avatar } from 'antd'
import { UserOutlined, HeartOutlined, HeartFilled, MessageOutlined, DeleteOutlined } from '@ant-design/icons'
import { useAtomValue } from 'jotai'
import { authAtom } from '@/stores/auth'
import type { PostSummary } from '@/apis/pms'
import useToggleLikeMutation from '@/queries/useToggleLikeMutation'
import { formatRelativeTime } from '@/lib/date'
import styles from './PostCard.module.css'

interface Props {
  post: PostSummary
  categoryNames?: string[]
  onClick?: () => void
  onDelete?: () => void
}

export default function PostCard({ post, categoryNames = [], onClick, onDelete }: Props) {
  const auth = useAtomValue(authAtom)
  const { message } = App.useApp()
  const { mutate: toggleLike } = useToggleLikeMutation()

  const liked = post.metrics.likedByMe

  const handleToggleLike = (e: React.MouseEvent) => {
    // 카드 클릭(상세 이동)과 분리
    e.stopPropagation()
    // 좋아요는 인증 필요 — 버튼 disabled로 차단되지만 방어적으로 한 번 더 확인
    if (!auth) return
    toggleLike(
      { section: post.section.toLowerCase(), id: post.id, like: !liked },
      {
        onError: () => {
          // 캐시 원복은 훅에서 처리 — 여기서는 에러 토스트만
          message.error('좋아요 처리에 실패했어요. 잠시 후 다시 시도해주세요.')
        },
      },
    )
  }

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
        <button
          type="button"
          className={`${styles.like} ${liked ? styles.liked : ''}`}
          aria-label="좋아요"
          aria-pressed={liked}
          disabled={!auth}
          onClick={handleToggleLike}
        >
          {liked ? <HeartFilled /> : <HeartOutlined />} {post.metrics.likeCount}
        </button>
        <span><MessageOutlined /> {post.metrics.commentCount}</span>
      </div>
    </div>
  )
}
