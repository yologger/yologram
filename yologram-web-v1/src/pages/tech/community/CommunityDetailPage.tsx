import { useEffect, useRef, useState } from 'react'
import { useNavigate, useParams } from 'react-router'
import { useAtom } from 'jotai'
import { useQueryClient } from '@tanstack/react-query'
import { App, Avatar } from 'antd'
import {
  ArrowLeftOutlined,
  UserOutlined,
  HeartOutlined,
  HeartFilled,
  MessageOutlined,
  RetweetOutlined,
  ShareAltOutlined,
  EditOutlined,
  DeleteOutlined,
} from '@ant-design/icons'
import { authAtom } from '../../../stores/auth'
import usePostQuery from '../../../queries/usePostQuery'
import useDeletePostMutation from '../../../queries/useDeletePostMutation'
import useTogglePostLikeMutation from '../../../queries/useTogglePostLikeMutation'
import useCreateCommentMutation from '../../../queries/useCreateCommentMutation'
import useUpdateCommentMutation from '../../../queries/useUpdateCommentMutation'
import useDeleteCommentMutation from '../../../queries/useDeleteCommentMutation'
import useCommentsQuery from '../../../queries/useCommentsQuery'
import type { CommentSort } from '../../../apis/comments'
import { getErrorStatus } from '../../../lib/error'
import styles from './CommunityDetailPage.module.css'

export default function CommunityDetailPage() {
  const { postId } = useParams()
  const navigate = useNavigate()
  const id = Number(postId)

  const { data: post, isLoading, isError, error, refetch } = usePostQuery('tech', id)
  const [auth] = useAtom(authAtom)
  const [text, setText] = useState('')
  const [sort, setSort] = useState<CommentSort>('latest')
  // 한 번에 하나의 댓글만 편집 (편집 중인 commentId + 편집 텍스트)
  const [editingId, setEditingId] = useState<number | null>(null)
  const [editText, setEditText] = useState('')
  const sentinelRef = useRef<HTMLDivElement>(null)
  const { message, modal } = App.useApp()
  const queryClient = useQueryClient()
  const { mutate: deletePost } = useDeletePostMutation()
  const { mutate: toggleLikeMutate } = useTogglePostLikeMutation()
  const { mutate: createComment, isPending: isSubmitting } = useCreateCommentMutation()
  const { mutate: updateComment, isPending: isUpdating } = useUpdateCommentMutation()
  const { mutate: deleteComment } = useDeleteCommentMutation()

  const {
    data: comments = [],
    fetchNextPage,
    hasNextPage,
    isFetchingNextPage,
    isLoading: isCommentsLoading,
    isError: isCommentsError,
    refetch: refetchComments,
  } = useCommentsQuery('tech', id, sort)

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

  if (isLoading) {
    return (
      <div className={styles.container}>
        <div className={styles.header}>
          <button className={styles.back} aria-label="뒤로" onClick={() => navigate(-1)}>
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
          <button className={styles.back} aria-label="뒤로" onClick={() => navigate(-1)}>
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
          <button className={styles.back} aria-label="뒤로" onClick={() => navigate(-1)}>
            <ArrowLeftOutlined />
          </button>
        </div>
        <div style={{ padding: 16 }}>존재하지 않는 글입니다.</div>
      </div>
    )
  }

  const authorName = post.author.nickname ?? '알 수 없음'
  // 본인 글일 때만 수정 노출 (상세 응답 author.uid 와 로그인 uid 비교)
  const isAuthor = auth != null && auth.uid === post.author.uid
  const createdAtText = new Date(post.createdAt).toLocaleString('ko-KR')
  const isAuthenticated = auth != null
  // 좋아요 상태는 서버 metrics(likedByMe/likeCount)가 원본 — 옵티미스틱 반영은 캐시에서 수행
  const { likeCount, commentCount, likedByMe } = post.metrics

  const toggleLike = () => {
    // 미인증 시 무시 (버튼도 동일 조건으로 비활성)
    if (!isAuthenticated) return
    toggleLikeMutate(
      { section: 'tech', id, like: !likedByMe },
      {
        onError: () => {
          // 캐시 원복은 뮤테이션 훅이 처리, 여기서는 토스트만
          message.error('좋아요 처리에 실패했어요. 잠시 후 다시 시도해주세요.')
        },
      },
    )
  }

  const handleDelete = () => {
    // 삭제는 되돌릴 수 없으므로 확인 모달을 거친다
    modal.confirm({
      title: '게시글을 삭제할까요?',
      content: '삭제한 글은 되돌릴 수 없어요.',
      okText: '삭제',
      okButtonProps: { danger: true },
      cancelText: '취소',
      onOk: () =>
        new Promise<void>((resolve) => {
          deletePost(
            { section: 'tech', id },
            {
              onSuccess: () => {
                // 피드/내 글 목록/상세 무효화 → 삭제 반영
                queryClient.invalidateQueries({ queryKey: ['posts', 'tech'] })
                queryClient.invalidateQueries({ queryKey: ['my-posts'] })
                queryClient.invalidateQueries({ queryKey: ['post', 'tech', id] })
                // 삭제된 글은 다시 볼 일이 없으므로 댓글 캐시는 제거
                queryClient.removeQueries({ queryKey: ['comments', 'tech', id] })
                message.success('글이 삭제되었습니다.')
                // 진입 출처(기술 커뮤니티 또는 내 글 목록)로 복귀 — 뒤로가기 버튼과 동일
                navigate(-1)
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
    // 미인증/빈 내용/전송 중에는 무시 (버튼도 동일 조건으로 비활성)
    if (!isAuthenticated || !text.trim() || isSubmitting) return
    createComment(
      { section: 'tech', postId: id, content: text.trim() },
      {
        onSuccess: () => {
          setText('')
          message.success('댓글이 등록되었습니다.')
          // 목록 무효화 → 작성한 댓글이 최신순 맨 위에 반영
          queryClient.invalidateQueries({ queryKey: ['comments', 'tech', id] })
          // 게시글 무효화 → commentCount 갱신 (서버가 카운트 테이블 조인으로 실값 반환)
          queryClient.invalidateQueries({ queryKey: ['post', 'tech', id] })
        },
        onError: () => {
          // reject를 남기면 unhandled rejection이 되므로 토스트만 띄우고 안전하게 종료
          message.error('댓글 등록에 실패했어요. 잠시 후 다시 시도해주세요.')
        },
      },
    )
  }

  const startEdit = (commentId: number, content: string) => {
    setEditingId(commentId)
    setEditText(content)
  }

  const cancelEdit = () => {
    // 편집 취소 시 원복 (편집 모드 종료 + 임시 텍스트 초기화)
    setEditingId(null)
    setEditText('')
  }

  const saveEdit = (commentId: number) => {
    // 빈 내용/전송 중에는 무시 (저장 버튼도 동일 조건으로 비활성)
    if (!editText.trim() || isUpdating) return
    updateComment(
      { section: 'tech', commentId, content: editText.trim() },
      {
        onSuccess: () => {
          setEditingId(null)
          setEditText('')
          message.success('댓글이 수정되었습니다.')
          // 목록 무효화 → 수정 내용 반영
          queryClient.invalidateQueries({ queryKey: ['comments', 'tech', id] })
        },
        onError: () => {
          // reject를 남기면 unhandled rejection이 되므로 토스트만 띄우고 안전하게 종료
          message.error('댓글 수정에 실패했어요. 잠시 후 다시 시도해주세요.')
        },
      },
    )
  }

  const handleDeleteComment = (commentId: number) => {
    // 삭제는 되돌릴 수 없으므로 확인 모달을 거친다 (게시글 삭제와 동일 패턴)
    modal.confirm({
      title: '댓글을 삭제할까요?',
      content: '삭제한 댓글은 되돌릴 수 없어요.',
      okText: '삭제',
      okButtonProps: { danger: true },
      cancelText: '취소',
      onOk: () =>
        new Promise<void>((resolve) => {
          deleteComment(
            { section: 'tech', commentId },
            {
              onSuccess: () => {
                message.success('댓글이 삭제되었습니다.')
                // 목록 무효화 → 삭제 반영
                queryClient.invalidateQueries({ queryKey: ['comments', 'tech', id] })
                // 게시글 무효화 → commentCount 갱신
                queryClient.invalidateQueries({ queryKey: ['post', 'tech', id] })
                resolve()
              },
              onError: () => {
                // 실패해도 모달은 닫고 에러 토스트만(reject 시 unhandled rejection 발생)
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
        <button className={styles.back} aria-label="뒤로" onClick={() => navigate(-1)}>
          <ArrowLeftOutlined />
        </button>
        {isAuthor && (
          <div className={styles.authorActions}>
            <button
              className={styles.edit}
              aria-label="수정"
              onClick={() => navigate(`/tech/community/${id}/edit`)}
            >
              <EditOutlined /> 수정
            </button>
            <button className={styles.delete} aria-label="삭제" onClick={handleDelete}>
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
          {/* 미인증 시 비활성 (댓글 입력 비활성 관례와 동일) */}
          <button
            className={`${styles.likeButton} ${likedByMe ? styles.liked : ''}`}
            aria-label="좋아요"
            aria-pressed={likedByMe}
            disabled={!isAuthenticated}
            onClick={toggleLike}
          >
            {likedByMe ? <HeartFilled /> : <HeartOutlined />} {likeCount}
          </button>
          <span className={styles.action}><MessageOutlined /> {commentCount}</span>
          <span className={styles.action}><RetweetOutlined /></span>
          <span className={styles.action}><ShareAltOutlined /></span>
        </div>
      </div>

      <div className={styles.commentsHeader}>
        <span className={styles.commentsTitle}>댓글 {commentCount}</span>
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

      {isCommentsLoading && <div className={styles.commentsState}>댓글을 불러오는 중…</div>}

      {isCommentsError && (
        <div className={styles.commentsState}>
          <p>댓글을 불러오지 못했어요.</p>
          <button className={styles.sendButton} onClick={() => refetchComments()}>다시 시도</button>
        </div>
      )}

      {!isCommentsLoading && !isCommentsError && comments.length === 0 && (
        <div className={styles.commentsState}>아직 댓글이 없어요. 첫 댓글을 남겨보세요.</div>
      )}

      {comments.map((c) => {
        // 본인 댓글일 때만 수정 노출 (게시글 isAuthor 패턴과 동일)
        const isCommentAuthor = auth != null && auth.uid === c.author.uid
        const isEditing = editingId === c.id
        return (
          <div key={c.id} className={styles.comment}>
            <div className={styles.authorRow}>
              <Avatar size={28} icon={<UserOutlined />} />
              <span className={styles.author}>{c.author.nickname ?? '알 수 없음'}</span>
              <span className={styles.time}>{new Date(c.createdAt).toLocaleString('ko-KR')}</span>
              {isCommentAuthor && !isEditing && (
                <>
                  <button
                    className={styles.commentEdit}
                    aria-label="댓글 수정"
                    onClick={() => startEdit(c.id, c.content)}
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
                </>
              )}
            </div>
            {isEditing ? (
              <div className={styles.commentEditBox}>
                <textarea
                  className={styles.commentEditInput}
                  aria-label="댓글 수정 입력"
                  value={editText}
                  maxLength={1000}
                  disabled={isUpdating}
                  onChange={(e) => setEditText(e.target.value)}
                />
                <div className={styles.commentEditActions}>
                  <button
                    className={styles.commentEditSave}
                    disabled={!editText.trim() || isUpdating}
                    onClick={() => saveEdit(c.id)}
                  >
                    저장
                  </button>
                  <button
                    className={styles.commentEditCancel}
                    disabled={isUpdating}
                    onClick={cancelEdit}
                  >
                    취소
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
          placeholder={isAuthenticated ? '댓글로 의견을 남겨보세요' : '로그인 후 댓글을 남길 수 있어요'}
          value={text}
          disabled={!isAuthenticated || isSubmitting}
          maxLength={1000}
          onChange={(e) => setText(e.target.value)}
          onKeyDown={(e) => {
            if (e.key === 'Enter') submitComment()
          }}
        />
        <button
          className={styles.sendButton}
          disabled={!isAuthenticated || !text.trim() || isSubmitting}
          onClick={submitComment}
        >
          등록
        </button>
      </div>
    </div>
  )
}
