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
  nextCursor?: string | null
}

export interface GetArticlesParams {
  // 카테고리 ID (게시판·아티클 공용 카테고리 마스터 기준)
  categoryId?: number | null
  cursor?: string | null
  size?: number
}

export async function getArticles(params: GetArticlesParams = {}): Promise<ArticlePage> {
  const query: Record<string, string | number> = {}
  if (params.categoryId != null) query.categoryId = params.categoryId
  if (params.cursor) query.cursor = params.cursor
  if (params.size) query.size = params.size

  const response = await api.get<ArticlePage>('/api/v2/articles/tech', { params: query })
  return response.data
}
