import { useState } from 'react'
import { Typography } from 'antd'
import FilterChips, { type ChipItem } from '../../components/common/FilterChips'
import usePostCategoriesQuery from '../../queries/usePostCategoriesQuery'

export default function TechArticlesPage() {
  const { 
    data: categories = [],  // data(카테고리 배열)를 categories로 rename.
    isLoading, 
    isError, 
    error
  } = usePostCategoriesQuery('tech')
  const [category, setCategory] = useState<number | null>(null)

  const items: Array<ChipItem<number | null>> = [
    { label: '전체', value: null },
    ...categories.map((c) => ({ label: c.name, value: c.id as number | null })),
  ]

  const selectedLabel = category === null
    ? '전체'
    : categories.find((c) => c.id === category)?.name ?? '전체'

  return (
    <div>
      <FilterChips items={items} selected={category} onChange={setCategory} />
      <Typography.Text type="secondary">{selectedLabel}</Typography.Text>
    </div>
  )
}
