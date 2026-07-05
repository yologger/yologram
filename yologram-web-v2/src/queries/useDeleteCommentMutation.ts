'use client'

import { useMutation } from '@tanstack/react-query'
import { deleteComment } from '@/apis/pms'

export default function useDeleteCommentMutation() {
  return useMutation({
    mutationFn: ({ commentId }: { commentId: number }) => deleteComment(commentId),
  })
}
