'use client'

import { useParams } from 'next/navigation'
import { Typography } from 'antd'
import SearchBar from './SearchBar'
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

// 섹션 키워드 검색결과 페이지 공용 컴포넌트 — tech/invest/politics 라우트가 얇게 감싼다
export default function SectionKeywordPage({ basePath }: SectionKeywordPageProps) {
  const params = useParams<{ keyword: string }>()
  const keyword = decodeKeyword(params.keyword)

  return (
    <div className={styles.container}>
      <SearchBar basePath={basePath} initialValue={keyword} />
      <Typography.Text className={styles.result}>{`'${keyword}' 검색결과`}</Typography.Text>
    </div>
  )
}
