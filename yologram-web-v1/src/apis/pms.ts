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

export interface UpdatePostRequest {
  title?: string | null
  content: string
  categoryIds: number[]
}

export async function updatePost(section: string, id: number, request: UpdatePostRequest): Promise<void> {
  await api.patch(`/api/v1/pms/${section}/posts/${id}`, request)
}

export async function deletePost(section: string, id: number): Promise<void> {
  await api.delete(`/api/v1/pms/${section}/posts/${id}`)
}

// 게시글 카운트 지표 — 목록/상세 공통 (likedByMe는 Authorization 헤더가 있을 때만 서버가 채움, 없으면 false)
export interface PostMetrics {
  commentCount: number
  likeCount: number
  likedByMe: boolean
}

export interface PostDetail {
  id: number
  section: string
  author: { uid: number; nickname: string | null }
  title?: string
  content: string
  categoryIds: number[]
  metrics: PostMetrics
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
  metrics: PostMetrics
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

export interface GetMyPostsParams {
  // 생략 시 전체 섹션 (값은 tech/invest/politics 소문자)
  section?: string | null
  cursor?: string | null
  size?: number
}

export async function getMyPosts(params: GetMyPostsParams = {}): Promise<PostPage> {
  const query: Record<string, string | number> = {}
  if (params.section) query.section = params.section
  if (params.cursor) query.cursor = params.cursor
  if (params.size) query.size = params.size

  const response = await api.get<PostPage>('/api/v1/pms/posts/me', { params: query })
  return response.data
}

// 좋아요 등록 — 인증 필수, 멱등 (이미 눌렀어도 200 no-op)
export async function likePost(section: string, id: number): Promise<void> {
  await api.post(`/api/v1/pms/${section}/posts/${id}/like`)
}

// 좋아요 취소 — 인증 필수, 멱등 (안 눌렀어도 200 no-op)
export async function unlikePost(section: string, id: number): Promise<void> {
  await api.delete(`/api/v1/pms/${section}/posts/${id}/like`)
}
