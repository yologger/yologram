'use client'

import { useMutation } from '@tanstack/react-query'
import { updateComment } from '@/apis/pms'

export default function useUpdateCommentMutation() {
  return useMutation({
    mutationFn: ({ commentId, content }: { commentId: number; content: string }) =>
      updateComment(commentId, content),
  })
}
