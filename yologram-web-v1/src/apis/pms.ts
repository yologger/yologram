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
  const response = await api.post<{ data: CreatePostResponse }>(`/api/v1/pms/${section}/posts`, request)
  return response.data.data
}
