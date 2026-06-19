import { useMutation } from '@tanstack/react-query'
import { createPost, type CreatePostRequest } from '../apis/pms'

export default function useCreatePostMutation() {
  return useMutation({
    mutationFn: ({ section, request }: { section: string; request: CreatePostRequest }) =>
      createPost(section, request),
  })
}
