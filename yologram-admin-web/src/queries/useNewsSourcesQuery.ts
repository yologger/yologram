import { useQuery } from '@tanstack/react-query'
import { getNewsSources } from '../apis/newsSources'

export default function useNewsSourcesQuery() {
  return useQuery({
    queryKey: ['newsSources'],
    queryFn: () => getNewsSources(),
  })
}
