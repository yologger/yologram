import api from '../lib/api'

export interface CreateCommentRequest {
  content: string
}

export interface CreateCommentResponse {
  id: number
}

export async function createComment(postId: number, content: string): Promise<CreateCommentResponse> {
  const response = await api.post<{ data: CreateCommentResponse }>(
    `/api/v1/comments/posts/${postId}`,
    { content } satisfies CreateCommentRequest,
  )
  return response.data.data
}
