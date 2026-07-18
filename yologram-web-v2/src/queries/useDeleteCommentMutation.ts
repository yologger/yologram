'use client'

import { useMutation } from '@tanstack/react-query'
import { deleteComment } from '@/apis/pms'

export default function useDeleteCommentMutation() {
  return useMutation({
    mutationFn: ({ section, commentId }: { section: string; commentId: number }) =>
      deleteComment(section, commentId),
  })
}
