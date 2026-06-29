'use client'

import { useMutation } from '@tanstack/react-query'
import { deletePost } from '@/apis/pms'

export default function useDeletePostMutation() {
  return useMutation({
    mutationFn: ({ section, id }: { section: string; id: number }) => deletePost(section, id),
  })
}
