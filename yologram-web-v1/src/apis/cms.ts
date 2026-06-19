import api from '../lib/api'

export interface Category {
  id: number
  name: string
  sortOrder: number
}

export async function getCategories(section: string): Promise<Category[]> {
  const response = await api.get<{ data: Category[] }>(`/api/v1/cms/${section}/categories`)
  return response.data.data
}
