import { useMutation } from '@tanstack/react-query'
import { indexPostRange, type IndexingSection } from '../apis/postIndexing'

export default function useIndexPostRangeMutation(section: IndexingSection) {
  return useMutation({
    mutationFn: ({ from, to }: { from: number; to: number }) => indexPostRange(section, from, to),
  })
}
