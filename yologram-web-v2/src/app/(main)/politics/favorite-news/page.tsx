'use client'

import { Typography } from 'antd'
import RequireAuth from '@/components/auth/RequireAuth'

export default function PoliticsFavoriteNews() {
  return (
    <RequireAuth>
      <Typography.Text>관심 뉴스</Typography.Text>
    </RequireAuth>
  )
}
