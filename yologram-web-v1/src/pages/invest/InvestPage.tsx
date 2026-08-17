import SubTabLayout from '../../components/common/SubTabLayout'

// 관심 뉴스·채용 준비 중: 구현 완료 시 아래 주석을 해제한다 (라우트는 그대로 살아 있어 직접 접근은 가능)
const tabs = [
  { key: 'news', label: '뉴스' },
  // { key: 'favorite-news', label: '관심 뉴스' },
  { key: 'community', label: '커뮤니티' },
  { key: 'info', label: '정보' },
]

export default function InvestPage() {
  return <SubTabLayout basePath="/invest" tabs={tabs} title="투자" />
}
