import { useMutation } from '@tanstack/react-query'
import { deleteNewsSource } from '../apis/newsSources'

export default function useDeleteNewsSourceMutation() {
  return useMutation({
    mutationFn: (id: number) => deleteNewsSource(id),
  })
}
