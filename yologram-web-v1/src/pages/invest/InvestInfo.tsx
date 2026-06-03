import { useState } from 'react'
import { Typography } from 'antd'
import FilterChips from '../../components/common/FilterChips'

const filters = ['시가총액', '거래대금', '거래량', '인기']

export default function InvestInfo() {
  const [selected, setSelected] = useState('시가총액')

  return (
    <div>
      <FilterChips items={filters} selected={selected} onChange={setSelected} />
      <Typography.Text type="secondary">{selected}</Typography.Text>
    </div>
  )
}
