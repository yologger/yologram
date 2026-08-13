import type { MouseEvent } from 'react'
import { App, Avatar } from 'antd'
import { UserOutlined, HeartOutlined, HeartFilled, MessageOutlined, EyeOutlined } from '@ant-design/icons'
import useTogglePostLikeMutation from '../../../queries/useTogglePostLikeMutation'
import useRequireAuth from '../../../hooks/useRequireAuth'
import type { PostSummary } from '../../../apis/pms'
import { formatRelativeTime } from '../../../lib/date'
import styles from './PostCard.module.css'

interface Props {
  post: PostSummary
  categoryNames?: string[]
  onClick?: () => void
}

export default function PostCard({ post, categoryNames = [], onClick }: Props) {
  const { message } = App.useApp()
  const { mutate: toggleLike } = useTogglePostLikeMutation()
  const requireAuth = useRequireAuth()
  const { likeCount, commentCount, viewCount, likedByMe } = post.metrics

  const handleToggleLike = (e: MouseEvent<HTMLButtonElement>) => {
    // 카드 클릭(상세 이동)과 분리
    e.stopPropagation()
    // 비로그인 시 로그인 유도 모달을 띄우고 중단
    if (!requireAuth()) return
    toggleLike(
      // section은 응답이 대문자(TECH)라 API 경로용 소문자로 변환
      { section: post.section.toLowerCase(), id: post.id, like: !likedByMe },
      {
        onError: () => {
          // 캐시 원복은 뮤테이션 훅이 처리, 여기서는 토스트만
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
        {/* 비로그인에도 활성 — 클릭 시 로그인 유도 모달 (useRequireAuth) */}
        <button
          className={`${styles.likeButton} ${likedByMe ? styles.liked : ''}`}
          aria-label="좋아요"
          aria-pressed={likedByMe}
          onClick={handleToggleLike}
        >
          {likedByMe ? <HeartFilled /> : <HeartOutlined />} {likeCount}
        </button>
        <span><MessageOutlined /> {commentCount}</span>
        <span><EyeOutlined /> {viewCount}</span>
      </div>
    </div>
  )
}
