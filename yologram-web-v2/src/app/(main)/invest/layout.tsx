import SubTabLayout from '@/components/common/SubTabLayout'

const tabs = [
  { key: 'news', label: '뉴스' },
  { key: 'community', label: '커뮤니티' },
]

export default function InvestLayout({ children }: { children: React.ReactNode }) {
  return <SubTabLayout basePath="/invest" tabs={tabs}>{children}</SubTabLayout>
}
