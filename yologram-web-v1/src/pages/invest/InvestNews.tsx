import { useState } from 'react'
import { Typography } from 'antd'
import FilterChips from '../../components/common/FilterChips'

const categories = ['전체', '국내', '해외']

export default function InvestNews() {
  const [category, setCategory] = useState('전체')

  return (
    <div>
      <FilterChips items={categories} selected={category} onChange={setCategory} />
      <Typography.Text type="secondary">{category}</Typography.Text>
    </div>
  )
}
