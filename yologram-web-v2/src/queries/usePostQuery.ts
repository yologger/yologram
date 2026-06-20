'use client'

import { useQuery } from '@tanstack/react-query'
import { getPostDetail } from '@/apis/pms'

export default function usePostQuery(section: string, id: number) {
  return useQuery({
    queryKey: ['post', section, id],
    queryFn: () => getPostDetail(section, id),
    enabled: Number.isFinite(id),
  })
}
