import { useMutation } from '@tanstack/react-query'
import { indexAllPosts, type IndexingSection } from '../apis/postIndexing'

export default function useIndexAllPostsMutation(section: IndexingSection) {
  return useMutation({ mutationFn: () => indexAllPosts(section) })
}
