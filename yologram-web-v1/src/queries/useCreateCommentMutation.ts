import { useMutation } from '@tanstack/react-query'
import { createComment } from '../apis/comments'

export default function useCreateCommentMutation() {
  return useMutation({
    mutationFn: ({ postId, content }: { postId: number; content: string }) =>
      createComment(postId, content),
  })
}
