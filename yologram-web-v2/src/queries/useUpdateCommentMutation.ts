'use client'

import { useMutation } from '@tanstack/react-query'
import { updateComment } from '@/apis/pms'

export default function useUpdateCommentMutation() {
  return useMutation({
    mutationFn: ({ section, commentId, content }: { section: string; commentId: number; content: string }) =>
      updateComment(section, commentId, content),
  })
}
