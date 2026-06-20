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
  const response = await api.get<{ data: PostDetail }>(`/api/v1/pms/${section}/posts/${id}`)
  return response.data.data
}

export interface PostSummary {
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

export interface PostPage {
  data: PostSummary[]
  nextCursor?: string | null
}

export interface GetPostsParams {
  cursor?: string | null
  size?: number
  categoryId?: number | null
}

export async function getPosts(section: string, params: GetPostsParams = {}): Promise<PostPage> {
  const query: Record<string, string | number> = {}
  if (params.cursor) query.cursor = params.cursor
  if (params.size) query.size = params.size
  if (params.categoryId != null) query.categoryId = params.categoryId

  const response = await api.get<PostPage>(`/api/v1/pms/${section}/posts`, { params: query })
  return response.data
}
