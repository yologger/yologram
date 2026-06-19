import { useQuery } from '@tanstack/react-query'
import { getCategories } from '@/apis/cms'

export default function useCategoriesQuery(section: string) {
  return useQuery({
    queryKey: ['categories', section],
    queryFn: () => getCategories(section),
    staleTime: 1000 * 60 * 30,
  })
}
