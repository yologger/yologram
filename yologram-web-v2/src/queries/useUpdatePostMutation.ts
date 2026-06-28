'use client'

import { useMutation } from '@tanstack/react-query'
import { updatePost, type UpdatePostRequest } from '@/apis/pms'

export default function useUpdatePostMutation() {
  return useMutation({
    mutationFn: ({ section, id, request }: { section: string; id: number; request: UpdatePostRequest }) =>
      updatePost(section, id, request),
  })
}
