'use client'

import { useEffect, useState } from 'react'
import { useParams, useRouter } from 'next/navigation'
import { useAtom, useAtomValue } from 'jotai'
import { useQueryClient } from '@tanstack/react-query'
import { App, Avatar } from 'antd'
import {
  ArrowLeftOutlined,
  EditOutlined,
  DeleteOutlined,
  UserOutlined,
  HeartOutlined,
  HeartFilled,
  MessageOutlined,
  RetweetOutlined,
  ShareAltOutlined,
} from '@ant-design/icons'
import { communityCommentsAtom } from '@/stores/community'
import { authAtom } from '@/stores/auth'
import type { CommunityComment } from '@/types/community'
import usePostQuery from '@/queries/usePostQuery'
import useDeletePostMutation from '@/queries/useDeletePostMutation'
import { getErrorStatus } from '@/lib/error'
import styles from './CommunityDetail.module.css'

export default function CommunityDetail() {
  const params = useParams<{ postId: string }>()
  const router = useRouter()
  const id = Number(params.postId)

  const { data: post, isLoading, isError, error, refetch } = usePostQuery('tech', id)
  const auth = useAtomValue(authAtom)
  const [comments, setComments] = useAtom(communityCommentsAtom)
  const [text, setText] = useState('')
  const { modal, message } = App.useApp()
  const queryClient = useQueryClient()
  const { mutate: deletePost } = useDeletePostMutation()

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
  const isOwner = !!auth && post.author.uid === auth.uid
  const authorName = post.author.nickname ?? '알 수 없음'
  const createdAtText = new Date(post.createdAt).toLocaleString('ko-KR')

  const toggleLike = () => {
    setLiked((prev) => !prev)
    setLikeCount((prev) => (liked ? prev - 1 : prev + 1))
  }

  const handleDelete = () => {
    modal.confirm({
      title: '글을 삭제할까요?',
      content: '삭제한 글은 복구할 수 없어요.',
      okText: '삭제',
      okButtonProps: { danger: true },
      cancelText: '취소',
      onOk: () =>
        new Promise<void>((resolve) => {
          deletePost(
            { section: 'tech', id },
            {
              onSuccess: () => {
                // 목록/내 글 재조회로 삭제 반영 후 목록으로 이동
                queryClient.invalidateQueries({ queryKey: ['posts', 'tech'] })
                queryClient.invalidateQueries({ queryKey: ['myPosts'] })
                queryClient.removeQueries({ queryKey: ['post', 'tech', id] })
                message.success('글이 삭제되었습니다.')
                // 진입 출처(기술 커뮤니티 또는 내 글 목록)로 복귀 — 뒤로가기 버튼과 동일
                router.back()
                resolve()
              },
              onError: () => {
                // 실패해도 모달은 닫고 에러 토스트만(reject 시 unhandled rejection 발생)
                message.error('글 삭제에 실패했어요. 잠시 후 다시 시도해주세요.')
                resolve()
              },
            },
          )
        }),
    })
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
        {isOwner && (
          <div className={styles.ownerActions}>
            <button
              className={styles.edit}
              aria-label="수정"
              onClick={() => router.push(`/tech/community/${id}/edit`)}
            >
              <EditOutlined /> 수정
            </button>
            <button
              className={styles.delete}
              aria-label="삭제"
              onClick={handleDelete}
            >
              <DeleteOutlined /> 삭제
            </button>
          </div>
        )}
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
