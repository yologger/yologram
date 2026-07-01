'use client'

import { useMutation } from '@tanstack/react-query'
import { createComment } from '@/apis/pms'

export default function useCreateCommentMutation() {
  return useMutation({
    mutationFn: ({ postId, content }: { postId: number; content: string }) =>
      createComment(postId, content),
  })
}
