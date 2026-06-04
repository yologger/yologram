'use client'

import { Typography } from 'antd'
import RequireAuth from '@/components/auth/RequireAuth'

export default function Notifications() {
  return (
    <RequireAuth>
      <Typography.Title level={3}>알림</Typography.Title>
    </RequireAuth>
  )
}
