import ComingSoon from '@/components/common/ComingSoon'

// 채용 준비 중: 구현 시작 시 ComingSoon 제거하고 아래 기존 page를 복구한다.
export default function TechJobs() {
  return <ComingSoon />
}

/*
'use client'

import { Typography } from 'antd'

export default function TechJobs() {
  return <Typography.Text>기술 채용</Typography.Text>
}
*/
