import { useMutation } from '@tanstack/react-query'
import { indexRange, type IndexingSection, type IndexingTarget } from '../apis/indexing'

export default function useIndexRangeMutation(section: IndexingSection, target: IndexingTarget) {
  return useMutation({
    mutationFn: ({ from, to }: { from: number; to: number }) =>
      indexRange(section, target, from, to),
  })
}
