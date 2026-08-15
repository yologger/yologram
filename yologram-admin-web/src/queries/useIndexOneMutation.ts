import { useMutation } from '@tanstack/react-query'
import { indexOne, type IndexingSection, type IndexingTarget } from '../apis/indexing'

export default function useIndexOneMutation(section: IndexingSection, target: IndexingTarget) {
  return useMutation({ mutationFn: (id: number) => indexOne(section, target, id) })
}
