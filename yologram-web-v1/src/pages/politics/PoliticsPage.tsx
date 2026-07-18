import SubTabLayout from '../../components/common/SubTabLayout'

const tabs = [
  { key: 'articles', label: '아티클' },
  { key: 'favorite-articles', label: '관심 아티클' },
  { key: 'community', label: '커뮤니티' },
  { key: 'info', label: '정보' },
]

export default function PoliticsPage() {
  return <SubTabLayout basePath="/politics" tabs={tabs} title="정치" />
}
