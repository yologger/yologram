'use client'

import { useState } from 'react'
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
import { techPostsAtom, techCommentsAtom } from '@/stores/techCommunity'
import type { CommunityComment } from '@/types/techCommunity'
import styles from './CommunityDetail.module.css'

export default function CommunityDetail() {
  const params = useParams<{ postId: string }>()
  const router = useRouter()
  const [posts, setPosts] = useAtom(techPostsAtom)
  const [comments, setComments] = useAtom(techCommentsAtom)
  const [text, setText] = useState('')

  const id = Number(params.postId)
  const post = posts.find((p) => p.id === id)

  const goBack = () => router.back()

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

  const toggleLike = () => {
    setPosts((prev) =>
      prev.map((p) =>
        p.id === id
          ? { ...p, liked: !p.liked, likeCount: p.liked ? p.likeCount - 1 : p.likeCount + 1 }
          : p,
      ),
    )
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
    setPosts((prev) => prev.map((p) => (p.id === id ? { ...p, commentCount: p.commentCount + 1 } : p)))
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
            <div className={styles.author}>{post.author}</div>
            <div className={styles.time}>{post.createdAt}</div>
          </div>
        </div>
        {post.title && <div className={styles.title}>{post.title}</div>}
        <div className={styles.content}>{post.content}</div>
        <div className={styles.actions}>
          <span className={`${styles.action} ${post.liked ? styles.liked : ''}`} onClick={toggleLike}>
            {post.liked ? <HeartFilled /> : <HeartOutlined />} {post.likeCount}
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
