import SubTabLayout from '@/components/common/SubTabLayout'

const tabs = [
  { key: 'news', label: '뉴스' },
  { key: 'favorite-news', label: '관심 뉴스' },
]

export default function TechLayout({ children }: { children: React.ReactNode }) {
  return <SubTabLayout basePath="/tech" tabs={tabs} title="기술">{children}</SubTabLayout>
}
