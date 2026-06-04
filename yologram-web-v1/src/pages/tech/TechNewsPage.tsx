import { useState } from 'react'
import { Typography } from 'antd'
import FilterChips from '../../components/common/FilterChips'

const categories = ['Frontend', 'Backend', 'AI/ML', 'DevOps', 'Cloud']

export default function TechNewsPage() {
  const [category, setCategory] = useState('Frontend')

  return (
    <div>
      <FilterChips items={categories} selected={category} onChange={setCategory} />
      <Typography.Text type="secondary">{category}</Typography.Text>
    </div>
  )
}
