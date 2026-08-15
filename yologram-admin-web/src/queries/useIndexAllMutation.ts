import { useMutation } from '@tanstack/react-query'
import { indexAll, type IndexingSection, type IndexingTarget } from '../apis/indexing'

export default function useIndexAllMutation(section: IndexingSection, target: IndexingTarget) {
  return useMutation({ mutationFn: () => indexAll(section, target) })
}
