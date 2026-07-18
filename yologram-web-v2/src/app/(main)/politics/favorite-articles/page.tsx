'use client'

import { Typography } from 'antd'
import RequireAuth from '@/components/auth/RequireAuth'

export default function PoliticsFavoriteArticles() {
  return (
    <RequireAuth>
      <Typography.Text>관심 아티클</Typography.Text>
    </RequireAuth>
  )
}
