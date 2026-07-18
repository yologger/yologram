'use client'

import { useEffect, useRef, useState } from 'react'
import { useParams, useRouter } from 'next/navigation'
import { useAtomValue } from 'jotai'
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
import { authAtom } from '@/stores/auth'
import type { CommentSort } from '@/apis/pms'
import usePostQuery from '@/queries/usePostQuery'
import useCommentsQuery from '@/queries/useCommentsQuery'
import useDeletePostMutation from '@/queries/useDeletePostMutation'
import useCreateCommentMutation from '@/queries/useCreateCommentMutation'
import useUpdateCommentMutation from '@/queries/useUpdateCommentMutation'
import useDeleteCommentMutation from '@/queries/useDeleteCommentMutation'
import { getErrorStatus } from '@/lib/error'
import styles from './CommunityDetail.module.css'

export default function CommunityDetail() {
  const params = useParams<{ postId: string }>()
  const router = useRouter()
  const id = Number(params.postId)

  const { data: post, isLoading, isError, error, refetch } = usePostQuery('tech', id)
  const auth = useAtomValue(authAtom)
  const [text, setText] = useState('')
  const [sort, setSort] = useState<CommentSort>('latest')
  const { modal, message } = App.useApp()
  const queryClient = useQueryClient()
  const { mutate: deletePost } = useDeletePostMutation()
  const { mutate: createComment, isPending: isCommentPending } = useCreateCommentMutation()
  const { mutate: updateComment, isPending: isUpdatePending } = useUpdateCommentMutation()
  const { mutate: deleteComment } = useDeleteCommentMutation()

  // 인라인 편집 중인 댓글 하나만 관리(commentId + 편집 텍스트)
  const [editingId, setEditingId] = useState<number | null>(null)
  const [editText, setEditText] = useState('')

  const {
    data: comments = [],
    fetchNextPage,
    hasNextPage,
    isFetchingNextPage,
    isLoading: isCommentsLoading,
    isError: isCommentsError,
  } = useCommentsQuery('tech', id, sort)
  const sentinelRef = useRef<HTMLDivElement>(null)

  useEffect(() => {
    if (!hasNextPage) return
    const el = sentinelRef.current
    if (!el) return

    const observer = new IntersectionObserver((entries) => {
      if (entries[0].isIntersecting && !isFetchingNextPage) {
        fetchNextPage()
      }
    })
    observer.observe(el)
    return () => observer.disconnect()
  }, [hasNextPage, isFetchingNextPage, fetchNextPage])

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
                // 삭제된 글은 다시 볼 일이 없으므로 댓글 캐시도 제거(백엔드도 함께 삭제)
                queryClient.removeQueries({ queryKey: ['comments', 'tech', id] })
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
    const content = text.trim()
    if (!content || isCommentPending) return

    // 댓글 작성은 인증 필요. 미로그인 시 안내만 하고 요청하지 않는다.
    if (!auth) {
      message.warning('로그인 후 댓글을 남길 수 있어요.')
      return
    }

    createComment(
      { section: 'tech', postId: id, content },
      {
        onSuccess: () => {
          // 작성 성공 시 해당 글의 댓글 목록 재조회(정렬 무관 전체 무효화)
          queryClient.invalidateQueries({ queryKey: ['comments', 'tech', id] })
          setText('')
          message.success('댓글이 등록되었습니다.')
        },
        onError: () => {
          // reject 시 unhandled rejection 발생 — 에러 토스트만 노출하고 콜백은 정상 종료
          message.error('댓글 등록에 실패했어요. 잠시 후 다시 시도해주세요.')
        },
      },
    )
  }

  const startEditComment = (commentId: number, content: string) => {
    setEditingId(commentId)
    setEditText(content)
  }

  const cancelEditComment = () => {
    // 취소 시 편집 상태만 종료(원본은 목록 데이터 그대로라 별도 원복 불필요)
    setEditingId(null)
    setEditText('')
  }

  const saveEditComment = () => {
    const content = editText.trim()
    if (editingId == null || !content || isUpdatePending) return

    updateComment(
      { section: 'tech', commentId: editingId, content },
      {
        onSuccess: () => {
          // 수정 성공 시 해당 글의 댓글 목록 재조회(정렬 무관 전체 무효화)
          queryClient.invalidateQueries({ queryKey: ['comments', 'tech', id] })
          setEditingId(null)
          setEditText('')
          message.success('댓글이 수정되었습니다.')
        },
        onError: () => {
          // reject 시 unhandled rejection 발생 — 에러 토스트만 노출하고 콜백은 정상 종료
          message.error('댓글 수정에 실패했어요. 잠시 후 다시 시도해주세요.')
        },
      },
    )
  }

  const handleDeleteComment = (commentId: number) => {
    modal.confirm({
      title: '댓글을 삭제할까요?',
      content: '삭제한 댓글은 복구할 수 없어요.',
      okText: '삭제',
      okButtonProps: { danger: true },
      cancelText: '취소',
      // 게시글 삭제와 동일 패턴: 성공/실패 모두 resolve(reject 시 unhandled rejection 발생)
      onOk: () =>
        new Promise<void>((resolve) => {
          deleteComment(
            { section: 'tech', commentId },
            {
              onSuccess: () => {
                // 삭제 성공 시 해당 글의 댓글 목록 재조회(정렬 무관 전체 무효화)
                queryClient.invalidateQueries({ queryKey: ['comments', 'tech', id] })
                // 편집 중이던 댓글을 삭제한 경우 편집 상태도 종료
                if (editingId === commentId) {
                  setEditingId(null)
                  setEditText('')
                }
                message.success('댓글이 삭제되었습니다.')
                resolve()
              },
              onError: () => {
                message.error('댓글 삭제에 실패했어요. 잠시 후 다시 시도해주세요.')
                resolve()
              },
            },
          )
        }),
    })
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

      <div className={styles.commentsHeader}>
        <span className={styles.commentsTitle}>댓글 {post.commentCount}</span>
        <div className={styles.sortToggle}>
          <button
            className={`${styles.sortButton} ${sort === 'latest' ? styles.sortActive : ''}`}
            onClick={() => setSort('latest')}
          >
            최신순
          </button>
          <button
            className={`${styles.sortButton} ${sort === 'oldest' ? styles.sortActive : ''}`}
            onClick={() => setSort('oldest')}
          >
            오래된순
          </button>
        </div>
      </div>

      {isCommentsLoading && <div className={styles.commentStatus}>댓글을 불러오는 중…</div>}
      {isCommentsError && (
        <div className={styles.commentStatus}>댓글을 불러오지 못했어요.</div>
      )}
      {!isCommentsLoading && !isCommentsError && comments.length === 0 && (
        <div className={styles.commentStatus}>첫 댓글을 남겨보세요.</div>
      )}

      {comments.map((c) => {
        const isCommentOwner = !!auth && c.author.uid === auth.uid
        const isEditing = editingId === c.id
        return (
          <div key={c.id} className={styles.comment}>
            <div className={styles.authorRow}>
              <Avatar size={28} icon={<UserOutlined />} />
              <span className={styles.author}>{c.author.nickname ?? '알 수 없음'}</span>
              <span className={styles.time}>{new Date(c.createdAt).toLocaleString('ko-KR')}</span>
              {isCommentOwner && !isEditing && (
                <div className={styles.commentActions}>
                  <button
                    className={styles.commentEdit}
                    aria-label="댓글 수정"
                    onClick={() => startEditComment(c.id, c.content)}
                  >
                    <EditOutlined /> 수정
                  </button>
                  <button
                    className={styles.commentDelete}
                    aria-label="댓글 삭제"
                    onClick={() => handleDeleteComment(c.id)}
                  >
                    <DeleteOutlined /> 삭제
                  </button>
                </div>
              )}
            </div>
            {isEditing ? (
              <div className={styles.editArea}>
                <textarea
                  className={styles.editInput}
                  value={editText}
                  onChange={(e) => setEditText(e.target.value)}
                  rows={2}
                />
                <div className={styles.editActions}>
                  <button className={styles.editCancel} onClick={cancelEditComment}>
                    취소
                  </button>
                  <button
                    className={styles.editSave}
                    disabled={!editText.trim() || isUpdatePending}
                    onClick={saveEditComment}
                  >
                    저장
                  </button>
                </div>
              </div>
            ) : (
              <div className={styles.content}>{c.content}</div>
            )}
          </div>
        )
      })}
      {hasNextPage && <div ref={sentinelRef} className={styles.sentinel} />}

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
        <button
          className={styles.sendButton}
          disabled={!text.trim() || isCommentPending}
          onClick={submitComment}
        >
          등록
        </button>
      </div>
    </div>
  )
}
