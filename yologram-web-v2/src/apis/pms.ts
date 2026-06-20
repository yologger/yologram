import api from '../lib/api'

export interface CreatePostRequest {
  title?: string
  content: string
  categoryIds: number[]
}

export interface CreatePostResponse {
  id: number
}

export async function createPost(section: string, request: CreatePostRequest): Promise<CreatePostResponse> {
  const response = await api.post<{ data: CreatePostResponse }>(`/api/v2/pms/${section}/posts`, request)
  return response.data.data
}

export interface PostDetail {
  id: number
  section: string
  author: { uid: number; nickname: string | null }
  title?: string
  content: string
  categoryIds: number[]
  likeCount: number
  commentCount: number
  createdAt: string
}

export async function getPostDetail(section: string, id: number): Promise<PostDetail> {
  const response = await api.get<{ data: PostDetail }>(`/api/v2/pms/${section}/posts/${id}`)
  return response.data.data
}
