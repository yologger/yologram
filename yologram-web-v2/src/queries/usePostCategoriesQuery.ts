import { useQuery } from '@tanstack/react-query'
import { getPostCategories } from '@/apis/cms'

export default function usePostCategoriesQuery(section: string) {
  return useQuery({
    queryKey: ['categories', section],
    queryFn: () => getPostCategories(section),
    staleTime: 1000 * 60 * 30,
  })
}
