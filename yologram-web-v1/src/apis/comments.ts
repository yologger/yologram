import api from '../lib/api'

export interface CreateCommentRequest {
  content: string
}

export interface CreateCommentResponse {
  id: number
}

export async function createComment(section: string, postId: number, content: string): Promise<CreateCommentResponse> {
  const response = await api.post<{ data: CreateCommentResponse }>(
    `/api/v1/comments/${section}/posts/${postId}`,
    { content } satisfies CreateCommentRequest,
  )
  return response.data.data
}

export interface UpdateCommentRequest {
  content: string
}

export async function updateComment(section: string, commentId: number, content: string): Promise<void> {
  await api.patch(
    `/api/v1/comments/${section}/${commentId}`,
    { content } satisfies UpdateCommentRequest,
  )
}

export async function deleteComment(section: string, commentId: number): Promise<void> {
  await api.delete(`/api/v1/comments/${section}/${commentId}`)
}

export type CommentSort = 'latest' | 'oldest'

export interface Comment {
  id: number
  postId: number
  author: { uid: number; nickname: string | null }
  content: string
  createdAt: string
}

export interface CommentPage {
  data: Comment[]
  nextCursor?: string | null
}

export interface GetCommentsParams {
  sort?: CommentSort
  cursor?: string | null
  size?: number
}

export async function getComments(section: string, postId: number, params: GetCommentsParams = {}): Promise<CommentPage> {
  const query: Record<string, string | number> = {}
  if (params.sort) query.sort = params.sort
  if (params.cursor) query.cursor = params.cursor
  if (params.size) query.size = params.size

  const response = await api.get<CommentPage>(`/api/v1/comments/${section}/posts/${postId}`, { params: query })
  return response.data
}
