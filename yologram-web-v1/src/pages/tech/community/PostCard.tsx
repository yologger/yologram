import type { MouseEvent } from 'react'
import { App, Avatar } from 'antd'
import { UserOutlined, HeartOutlined, HeartFilled, MessageOutlined } from '@ant-design/icons'
import { useAtom } from 'jotai'
import { authAtom } from '../../../stores/auth'
import useTogglePostLikeMutation from '../../../queries/useTogglePostLikeMutation'
import type { PostSummary } from '../../../apis/pms'
import { formatRelativeTime } from '../../../lib/date'
import styles from './PostCard.module.css'

interface Props {
  post: PostSummary
  categoryNames?: string[]
  onClick?: () => void
}

export default function PostCard({ post, categoryNames = [], onClick }: Props) {
  const [auth] = useAtom(authAtom)
  const { message } = App.useApp()
  const { mutate: toggleLike } = useTogglePostLikeMutation()
  const { likeCount, commentCount, likedByMe } = post.metrics

  const handleToggleLike = (e: MouseEvent<HTMLButtonElement>) => {
    // 카드 클릭(상세 이동)과 분리
    e.stopPropagation()
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
        {/* 미인증 시 비활성 (댓글 입력 비활성 관례와 동일) */}
        <button
          className={`${styles.likeButton} ${likedByMe ? styles.liked : ''}`}
          aria-label="좋아요"
          aria-pressed={likedByMe}
          disabled={auth == null}
          onClick={handleToggleLike}
        >
          {likedByMe ? <HeartFilled /> : <HeartOutlined />} {likeCount}
        </button>
        <span><MessageOutlined /> {commentCount}</span>
      </div>
    </div>
  )
}
