'use client'

import { Segmented } from 'antd'

/** 정렬 기준 — 백엔드 sort 파라미터와 같은 값 */
export type SearchSort = 'RELEVANCE' | 'LATEST'

interface Props {
  value: SearchSort
  onChange: (sort: SearchSort) => void
}

/** 연관도순·최신순 전환. 탭마다 독립적으로 갖는다(대상별로 원하는 정렬이 다르다) */
export default function SearchResultSort({ value, onChange }: Props) {
  return (
    <Segmented
      size="small"
      value={value}
      onChange={(v) => onChange(v as SearchSort)}
      options={[
        { label: '연관도순', value: 'RELEVANCE' },
        { label: '최신순', value: 'LATEST' },
      ]}
    />
  )
}
