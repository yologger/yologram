import api from '../lib/api'

export interface NewsSource {
  id: number
  name: string
  url: string
  isActive: boolean
  createdAt: string
  modifiedDate: string
}

export interface CreateNewsSourceRequest {
  name: string
  url: string
  isActive?: boolean
}

export interface UpdateNewsSourceRequest {
  name?: string
  url?: string
  isActive?: boolean
}

const BASE_PATH = '/api/v1/news/admin/tech/sources'

export async function getNewsSources(): Promise<NewsSource[]> {
  const response = await api.get<{ data: NewsSource[] }>(BASE_PATH)
  return response.data.data
}

export async function createNewsSource(request: CreateNewsSourceRequest): Promise<NewsSource> {
  const response = await api.post<{ data: NewsSource }>(BASE_PATH, request)
  return response.data.data
}

export async function updateNewsSource(id: number, request: UpdateNewsSourceRequest): Promise<NewsSource> {
  const response = await api.patch<{ data: NewsSource }>(`${BASE_PATH}/${id}`, request)
  return response.data.data
}

export async function deleteNewsSource(id: number): Promise<void> {
  await api.delete(`${BASE_PATH}/${id}`)
}
