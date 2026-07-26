'use client'

import { Typography } from 'antd'
import RequireAuth from '@/components/auth/RequireAuth'

export default function InvestFavoriteArticles() {
  return (
    <RequireAuth>
      <Typography.Text>관심 아티클</Typography.Text>
    </RequireAuth>
  )
}
