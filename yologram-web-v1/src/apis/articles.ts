import api from '../lib/api'

export interface Article {
  id: number
  title: string
  summary: string
  link: string
  sourceName: string
  categories: string[]
  publishedAt: string
}

export interface ArticlePage {
  data: Article[]
  // 마지막 페이지면 응답에서 생략됨
  nextCursor?: string | null
}

export interface GetArticlesParams {
  // 카테고리 ID (카테고리 API의 id, null이면 전체)
  categoryId?: number | null
  cursor?: string | null
  size?: number
}

export async function getArticles(params: GetArticlesParams = {}): Promise<ArticlePage> {
  const query: Record<string, string | number> = {}
  if (params.categoryId != null) query.categoryId = params.categoryId
  if (params.cursor) query.cursor = params.cursor
  if (params.size) query.size = params.size

  const response = await api.get<ArticlePage>('/api/v1/articles/tech', { params: query })
  return response.data
}
