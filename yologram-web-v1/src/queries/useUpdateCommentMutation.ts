import { useMutation } from '@tanstack/react-query'
import { updateComment } from '../apis/comments'

export default function useUpdateCommentMutation() {
  return useMutation({
    mutationFn: ({ commentId, content }: { commentId: number; content: string }) =>
      updateComment(commentId, content),
  })
}
