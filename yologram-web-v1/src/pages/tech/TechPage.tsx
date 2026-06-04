import SubTabLayout from '../../components/common/SubTabLayout'

const tabs = [
  { key: 'news', label: '뉴스' },
  { key: 'favorite-news', label: '관심 뉴스' },
]

export default function TechPage() {
  return <SubTabLayout basePath="/tech" tabs={tabs} />
}
