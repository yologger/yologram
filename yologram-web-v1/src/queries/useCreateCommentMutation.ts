import { useMutation } from '@tanstack/react-query'
import { createComment } from '../apis/comments'

export default function useCreateCommentMutation() {
  return useMutation({
    mutationFn: ({ section, postId, content }: { section: string; postId: number; content: string }) =>
      createComment(section, postId, content),
  })
}
