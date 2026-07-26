import api from '../lib/api'

export interface News {
  id: number
  title: string
  summary: string
  link: string
  sourceName: string
  categories: string[]
  publishedAt: string
}

export interface NewsPage {
  data: News[]
  // 마지막 페이지면 응답에서 생략됨
  nextCursor?: string | null
}

export interface GetNewsParams {
  // 카테고리 ID (카테고리 API의 id, null이면 전체)
  categoryId?: number | null
  cursor?: string | null
  size?: number
}

export async function getNews(params: GetNewsParams = {}): Promise<NewsPage> {
  const query: Record<string, string | number> = {}
  if (params.categoryId != null) query.categoryId = params.categoryId
  if (params.cursor) query.cursor = params.cursor
  if (params.size) query.size = params.size

  const response = await api.get<NewsPage>('/api/v1/news/tech', { params: query })
  return response.data
}
