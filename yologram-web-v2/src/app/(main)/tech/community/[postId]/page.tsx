'use client'

import { useEffect, useState } from 'react'
import { useParams, useRouter } from 'next/navigation'
import { useAtom } from 'jotai'
import { Avatar } from 'antd'
import {
  ArrowLeftOutlined,
  UserOutlined,
  HeartOutlined,
  HeartFilled,
  MessageOutlined,
  RetweetOutlined,
  ShareAltOutlined,
} from '@ant-design/icons'
import { communityCommentsAtom } from '@/stores/community'
import type { CommunityComment } from '@/types/community'
import usePostQuery from '@/queries/usePostQuery'
import { getErrorStatus } from '@/lib/error'
import styles from './CommunityDetail.module.css'

export default function CommunityDetail() {
  const params = useParams<{ postId: string }>()
  const router = useRouter()
  const id = Number(params.postId)

  const { data: post, isLoading, isError, error, refetch } = usePostQuery('tech', id)
  const [comments, setComments] = useAtom(communityCommentsAtom)
  const [text, setText] = useState('')

  // 좋아요는 서버 API(count 도메인) 도입 전까지 로컬 임시 상태
  const [liked, setLiked] = useState(false)
  const [likeCount, setLikeCount] = useState(0)
  useEffect(() => {
    if (post) {
      setLiked(false)
      setLikeCount(post.likeCount)
    }
  }, [post])

  const goBack = () => router.back()

  if (isLoading) {
    return (
      <div className={styles.container}>
        <div className={styles.header}>
          <button className={styles.back} aria-label="뒤로" onClick={goBack}>
            <ArrowLeftOutlined />
          </button>
        </div>
        <div style={{ padding: 16 }}>불러오는 중…</div>
      </div>
    )
  }

  // 404가 아닌 에러(네트워크/서버 오류)는 다시 시도 안내
  if (isError && getErrorStatus(error) !== 404) {
    return (
      <div className={styles.container}>
        <div className={styles.header}>
          <button className={styles.back} aria-label="뒤로" onClick={goBack}>
            <ArrowLeftOutlined />
          </button>
        </div>
        <div style={{ padding: 16 }}>
          <p>글을 불러오지 못했어요. 잠시 후 다시 시도해주세요.</p>
          <button className={styles.sendButton} onClick={() => refetch()}>다시 시도</button>
        </div>
      </div>
    )
  }

  // 404 또는 글 없음
  if (!post) {
    return (
      <div className={styles.container}>
        <div className={styles.header}>
          <button className={styles.back} aria-label="뒤로" onClick={goBack}>
            <ArrowLeftOutlined />
          </button>
        </div>
        <div style={{ padding: 16 }}>존재하지 않는 글입니다.</div>
      </div>
    )
  }

  const postComments = comments.filter((c) => c.postId === id)
  const authorName = post.author.nickname ?? '알 수 없음'
  const createdAtText = new Date(post.createdAt).toLocaleString('ko-KR')

  const toggleLike = () => {
    setLiked((prev) => !prev)
    setLikeCount((prev) => (liked ? prev - 1 : prev + 1))
  }

  const submitComment = () => {
    if (!text.trim()) return
    const newComment: CommunityComment = {
      id: Date.now(),
      postId: id,
      author: '나',
      createdAt: '방금 전',
      content: text.trim(),
      likeCount: 0,
    }
    setComments((prev) => [...prev, newComment])
    setText('')
  }

  return (
    <div className={styles.container}>
      <div className={styles.header}>
        <button className={styles.back} aria-label="뒤로" onClick={goBack}>
          <ArrowLeftOutlined />
        </button>
      </div>

      <div className={styles.post}>
        <div className={styles.authorRow}>
          <Avatar size={40} icon={<UserOutlined />} />
          <div>
            <div className={styles.author}>{authorName}</div>
            <div className={styles.time}>{createdAtText}</div>
          </div>
        </div>
        {post.title && <div className={styles.title}>{post.title}</div>}
        <div className={styles.content}>{post.content}</div>
        <div className={styles.actions}>
          <span className={`${styles.action} ${liked ? styles.liked : ''}`} onClick={toggleLike}>
            {liked ? <HeartFilled /> : <HeartOutlined />} {likeCount}
          </span>
          <span className={styles.action}><MessageOutlined /> {post.commentCount}</span>
          <span className={styles.action}><RetweetOutlined /></span>
          <span className={styles.action}><ShareAltOutlined /></span>
        </div>
      </div>

      <div className={styles.commentsTitle}>댓글 {postComments.length}</div>
      {postComments.map((c) => (
        <div key={c.id} className={styles.comment}>
          <div className={styles.authorRow}>
            <Avatar size={28} icon={<UserOutlined />} />
            <span className={styles.author}>{c.author}</span>
            <span className={styles.time}>{c.createdAt}</span>
          </div>
          <div className={styles.content}>{c.content}</div>
        </div>
      ))}

      <div className={styles.commentBar}>
        <input
          className={styles.commentInput}
          placeholder="댓글로 의견을 남겨보세요"
          value={text}
          onChange={(e) => setText(e.target.value)}
          onKeyDown={(e) => {
            if (e.key === 'Enter') submitComment()
          }}
        />
        <button className={styles.sendButton} disabled={!text.trim()} onClick={submitComment}>
          등록
        </button>
      </div>
    </div>
  )
}
