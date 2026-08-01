import { useMutation } from '@tanstack/react-query'
import { updateNewsSource, type UpdateNewsSourceRequest } from '../apis/newsSources'

export default function useUpdateNewsSourceMutation() {
  return useMutation({
    mutationFn: ({ id, request }: { id: number; request: UpdateNewsSourceRequest }) =>
      updateNewsSource(id, request),
  })
}
