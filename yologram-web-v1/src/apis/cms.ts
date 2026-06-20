import api from '../lib/api'

export interface PostCategory {
  id: number
  name: string
  sortOrder: number
}

export async function getPostCategories(section: string): Promise<PostCategory[]> {
  const response = await api.get<{ data: PostCategory[] }>(`/api/v1/cms/${section}/categories`)
  return response.data.data
}
