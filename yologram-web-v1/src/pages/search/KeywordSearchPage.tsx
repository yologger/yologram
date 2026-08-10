import { useParams } from 'react-router'
import { Typography } from 'antd'
import SearchBar from '../../components/common/SearchBar'
import styles from './KeywordSearchPage.module.css'

interface KeywordSearchPageProps {
  /** 섹션 기본 경로 (예: "/tech") */
  basePath: string
}

// 섹션 공용 키워드 검색 결과 페이지 (백엔드 연동 전 placeholder)
export default function KeywordSearchPage({ basePath }: KeywordSearchPageProps) {
  // React Router가 경로 파라미터를 자동 디코딩함 (%EC%A0%9C... → 제미나이)
  const { keyword = '' } = useParams<{ keyword: string }>()

  return (
    <div>
      <div className={styles.searchArea}>
        {/* key: 재검색으로 키워드가 바뀌면 입력값을 새 키워드로 재초기화 */}
        <SearchBar key={keyword} basePath={basePath} initialValue={keyword} />
      </div>
      <Typography.Text>{`'${keyword}' 검색결과`}</Typography.Text>
    </div>
  )
}
