import SubTabLayout from '@/components/common/SubTabLayout'

const tabs = [
  { key: 'articles', label: '아티클' },
  { key: 'favorite-articles', label: '관심 아티클' },
  { key: 'community', label: '커뮤니티' },
  { key: 'jobs', label: '채용' },
]

export default function TechLayout({ children }: { children: React.ReactNode }) {
  return <SubTabLayout basePath="/tech" tabs={tabs} title="기술" collapseOnScroll>{children}</SubTabLayout>
}
