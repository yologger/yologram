import { useMutation } from '@tanstack/react-query'
import { deleteComment } from '../apis/comments'

export default function useDeleteCommentMutation() {
  return useMutation({
    mutationFn: ({ commentId }: { commentId: number }) => deleteComment(commentId),
  })
}
