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

export interface UpdatePostRequest {
  title?: string | null
  content: string
  categoryIds: number[]
}

export async function updatePost(section: string, id: number, request: UpdatePostRequest): Promise<void> {
  await api.patch(`/api/v2/pms/${section}/posts/${id}`, request)
}

export async function deletePost(section: string, id: number): Promise<void> {
  await api.delete(`/api/v2/pms/${section}/posts/${id}`)
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

  const response = await api.get<PostPage>(`/api/v2/pms/${section}/posts`, { params: query })
  return response.data
}

export interface GetMyPostsParams {
  // section 생략 시 전체 조회. 값은 소문자(tech/invest/politics)
  section?: string | null
  cursor?: string | null
  size?: number
}

export async function getMyPosts(params: GetMyPostsParams = {}): Promise<PostPage> {
  const query: Record<string, string | number> = {}
  if (params.section) query.section = params.section
  if (params.cursor) query.cursor = params.cursor
  if (params.size) query.size = params.size

  const response = await api.get<PostPage>('/api/v2/pms/posts/me', { params: query })
  return response.data
}

export interface CreateCommentResponse {
  id: number
}

export async function createComment(section: string, postId: number, content: string): Promise<CreateCommentResponse> {
  const response = await api.post<{ data: CreateCommentResponse }>(
    `/api/v2/comments/${section}/posts/${postId}`,
    { content },
  )
  return response.data.data
}

export async function updateComment(section: string, commentId: number, content: string): Promise<void> {
  await api.patch(`/api/v2/comments/${section}/${commentId}`, { content })
}

export async function deleteComment(section: string, commentId: number): Promise<void> {
  await api.delete(`/api/v2/comments/${section}/${commentId}`)
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

  const response = await api.get<CommentPage>(`/api/v2/comments/${section}/posts/${postId}`, { params: query })
  return response.data
}
