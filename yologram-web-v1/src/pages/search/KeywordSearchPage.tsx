import { useParams } from 'react-router'
import { Tabs, Typography } from 'antd'
import SearchBar from '../../components/common/SearchBar'
import PostSearchResults from '../../components/search/PostSearchResults'
import NewsSearchResults from '../../components/search/NewsSearchResults'
import { MOCK_POST_TOTAL, MOCK_NEWS_TOTAL } from '../../components/search/searchResultMock'
import styles from './KeywordSearchPage.module.css'

interface KeywordSearchPageProps {
  /** 섹션 기본 경로 (예: "/tech") */
  basePath: string
}

/**
 * 섹션 공용 키워드 검색 결과 페이지 (web-v2 SectionKeywordPage 미러).
 *
 * 검색 대상별로 탭을 나눈다(커뮤니티·뉴스). 한 목록에 섞지 않는 이유는 스키마가 다르고
 * _score를 인덱스 간 비교할 수 없기 때문이다 — 각 탭이 자기 페이징·정렬을 갖는다.
 * 섹션 탭(뉴스·관심뉴스·커뮤니티·채용)과는 별개다: 여기엔 검색 가능한 대상만 올라온다.
 */
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

      <Tabs
        className={styles.tabs}
        defaultActiveKey="posts"
        items={[
          {
            key: 'posts',
            label: `커뮤니티 ${MOCK_POST_TOTAL}`,
            children: <PostSearchResults keyword={keyword} basePath={basePath} />,
          },
          {
            key: 'news',
            label: `뉴스 ${MOCK_NEWS_TOTAL}`,
            children: <NewsSearchResults keyword={keyword} />,
          },
        ]}
      />
    </div>
  )
}
