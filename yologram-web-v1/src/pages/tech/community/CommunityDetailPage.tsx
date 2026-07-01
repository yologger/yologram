import { useEffect, useState } from 'react'
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
import { communityCommentsAtom } from '../../../stores/community'
import usePostQuery from '../../../queries/usePostQuery'
import useDeletePostMutation from '../../../queries/useDeletePostMutation'
import useCreateCommentMutation from '../../../queries/useCreateCommentMutation'
import { getErrorStatus } from '../../../lib/error'
import styles from './CommunityDetailPage.module.css'

export default function CommunityDetailPage() {
  const { postId } = useParams()
  const navigate = useNavigate()
  const id = Number(postId)

  const { data: post, isLoading, isError, error, refetch } = usePostQuery('tech', id)
  const [auth] = useAtom(authAtom)
  const [comments] = useAtom(communityCommentsAtom)
  const [text, setText] = useState('')
  const { message, modal } = App.useApp()
  const queryClient = useQueryClient()
  const { mutate: deletePost } = useDeletePostMutation()
  const { mutate: createComment, isPending: isSubmitting } = useCreateCommentMutation()

  // 좋아요는 서버 API(count 도메인) 도입 전까지 로컬 임시 상태
  const [liked, setLiked] = useState(false)
  const [likeCount, setLikeCount] = useState(0)
  useEffect(() => {
    if (post) {
      setLiked(false)
      setLikeCount(post.likeCount)
    }
  }, [post])

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

  const postComments = comments.filter((c) => c.postId === id)
  const authorName = post.author.nickname ?? '알 수 없음'
  // 본인 글일 때만 수정 노출 (상세 응답 author.uid 와 로그인 uid 비교)
  const isAuthor = auth != null && auth.uid === post.author.uid
  const createdAtText = new Date(post.createdAt).toLocaleString('ko-KR')

  const toggleLike = () => {
    setLiked((prev) => !prev)
    setLikeCount((prev) => (liked ? prev - 1 : prev + 1))
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

  const isAuthenticated = auth != null

  const submitComment = () => {
    // 미인증/빈 내용/전송 중에는 무시 (버튼도 동일 조건으로 비활성)
    if (!isAuthenticated || !text.trim() || isSubmitting) return
    // 댓글 조회 API가 아직 없어 목록 갱신은 하지 않는다. 성공 피드백 + 입력창 초기화만.
    createComment(
      { postId: id, content: text.trim() },
      {
        onSuccess: () => {
          setText('')
          message.success('댓글이 등록되었습니다.')
        },
        onError: () => {
          // reject를 남기면 unhandled rejection이 되므로 토스트만 띄우고 안전하게 종료
          message.error('댓글 등록에 실패했어요. 잠시 후 다시 시도해주세요.')
        },
      },
    )
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
