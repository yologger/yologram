import ComingSoon from '@/components/common/ComingSoon'

// 투자 준비 중: layout이 children(하위 page)을 렌더하지 않고 ComingSoon으로 가로챈다.
// 구현 시작 시 이 함수를 제거하고 아래 주석 블록을 복구한다.
export default function InvestLayout() {
  return <ComingSoon title="투자" />
}

/*
import SubTabLayout from '@/components/common/SubTabLayout'

const tabs = [
  { key: 'articles', label: '아티클' },
  { key: 'favorite-articles', label: '관심 아티클' },
  { key: 'community', label: '커뮤니티' },
  { key: 'info', label: '정보' },
]

export default function InvestLayout({ children }: { children: React.ReactNode }) {
  return <SubTabLayout basePath="/invest" tabs={tabs} title="투자">{children}</SubTabLayout>
}
*/
