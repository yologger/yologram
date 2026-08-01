import { useMutation } from '@tanstack/react-query'
import { createNewsSource, type CreateNewsSourceRequest } from '../apis/newsSources'

export default function useCreateNewsSourceMutation() {
  return useMutation({
    mutationFn: (request: CreateNewsSourceRequest) => createNewsSource(request),
  })
}
