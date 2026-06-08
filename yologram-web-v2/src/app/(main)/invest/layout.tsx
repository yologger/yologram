import SubTabLayout from '@/components/common/SubTabLayout'

const tabs = [
  { key: 'news', label: '뉴스' },
  { key: 'favorite-news', label: '관심 뉴스' },
  { key: 'community', label: '커뮤니티' },
  { key: 'info', label: '정보' },
]

export default function InvestLayout({ children }: { children: React.ReactNode }) {
  return <SubTabLayout basePath="/invest" tabs={tabs} title="투자">{children}</SubTabLayout>
}
