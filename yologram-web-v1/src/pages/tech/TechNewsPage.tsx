import { useState } from 'react'
import { Typography } from 'antd'
import FilterChips from '../../components/common/FilterChips'
import { TECH_FILTER_CATEGORIES, ALL_CATEGORY } from '../../constants/techCategories'

export default function TechNewsPage() {
  const [category, setCategory] = useState(ALL_CATEGORY)

  return (
    <div>
      <FilterChips items={TECH_FILTER_CATEGORIES} selected={category} onChange={setCategory} />
      <Typography.Text type="secondary">{category}</Typography.Text>
    </div>
  )
}
