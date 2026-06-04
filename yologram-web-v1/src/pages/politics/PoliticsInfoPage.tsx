import { useState } from 'react'
import { Typography } from 'antd'
import FilterChips from '../../components/common/FilterChips'

const filters = ['정당', '국회의원', '의정활동', '인기']

export default function PoliticsInfoPage() {
  const [selected, setSelected] = useState('정당')

  return (
    <div>
      <FilterChips items={filters} selected={selected} onChange={setSelected} />
      <Typography.Text type="secondary">{selected}</Typography.Text>
    </div>
  )
}
