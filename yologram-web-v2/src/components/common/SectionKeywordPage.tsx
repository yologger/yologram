'use client'

import { useParams } from 'next/navigation'
import { Tabs, Typography } from 'antd'
import SearchBar from './SearchBar'
import PostSearchResults from '@/components/search/PostSearchResults'
import NewsSearchResults from '@/components/search/NewsSearchResults'
import { MOCK_POST_TOTAL, MOCK_NEWS_TOTAL } from '@/components/search/searchResultMock'
import styles from './SectionKeywordPage.module.css'

interface SectionKeywordPageProps {
  basePath: string
}

// dynamic params는 URL 인코딩된 상태로 오므로 디코딩 필요(한글 깨짐 방지).
// 디코딩 불가한 값(예: 원문에 %가 포함)은 원본 그대로 사용.
function decodeKeyword(raw: string) {
  try {
    return decodeURIComponent(raw)
  } catch {
    return raw
  }
}

/**
 * 섹션 키워드 검색결과 페이지 공용 컴포넌트 — tech/invest/politics 라우트가 얇게 감싼다.
 *
 * 검색 대상별로 탭을 나눈다(커뮤니티·뉴스). 한 목록에 섞지 않는 이유는 스키마가 다르고
 * _score를 인덱스 간 비교할 수 없기 때문이다 — 각 탭이 자기 페이징·정렬을 갖는다.
 * 홈의 탭(뉴스·관심뉴스·커뮤니티·채용)과는 별개다: 여기엔 검색 가능한 대상만 올라온다.
 */
export default function SectionKeywordPage({ basePath }: SectionKeywordPageProps) {
  const params = useParams<{ keyword: string }>()
  const keyword = decodeKeyword(params.keyword)

  return (
    <div className={styles.container}>
      <SearchBar basePath={basePath} initialValue={keyword} />
      <Typography.Text className={styles.result}>{`'${keyword}' 검색결과`}</Typography.Text>

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
