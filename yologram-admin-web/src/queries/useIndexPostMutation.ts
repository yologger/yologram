import { useMutation } from '@tanstack/react-query'
import { indexPost, type IndexingSection } from '../apis/postIndexing'

export default function useIndexPostMutation(section: IndexingSection) {
  return useMutation({ mutationFn: (id: number) => indexPost(section, id) })
}
