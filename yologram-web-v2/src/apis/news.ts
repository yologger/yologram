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
  nextCursor?: string | null
}

export interface GetNewsParams {
  // 카테고리 ID (게시판·뉴스 공용 카테고리 마스터 기준)
  categoryId?: number | null
  cursor?: string | null
  size?: number
}

export async function getNews(params: GetNewsParams = {}): Promise<NewsPage> {
  const query: Record<string, string | number> = {}
  if (params.categoryId != null) query.categoryId = params.categoryId
  if (params.cursor) query.cursor = params.cursor
  if (params.size) query.size = params.size

  const response = await api.get<NewsPage>('/api/v2/news/tech', { params: query })
  return response.data
}
