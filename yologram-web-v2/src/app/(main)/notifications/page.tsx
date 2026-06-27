import ComingSoon from '@/components/common/ComingSoon'

// 알림 준비 중: 구현 시작 시 ComingSoon 제거하고 아래 기존 page를 복구한다.
export default function Notifications() {
  return <ComingSoon />
}

/*
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
*/
