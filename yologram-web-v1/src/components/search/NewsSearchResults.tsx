import { useState } from 'react'
import { Alert, Empty, Pagination, Skeleton } from 'antd'
import NewsCard from '../news/NewsCard'
import useNewsSearchQuery from '../../queries/useNewsSearchQuery'
import { SEARCH_PAGE_SIZE } from '../../queries/usePostSearchQuery'
import { getErrorMessage } from '../../lib/error'
import SearchResultSort from './SearchResultSort'
import type { SearchSort } from '../../apis/search'
import styles from './SearchResults.module.css'

interface Props {
  keyword: string
  /** 검색 대상 섹션 (예: "tech") */
  section: string
}

/**
 * 뉴스 검색 결과 — 게시글 결과(PostSearchResults)와 같은 구조이고 카드·데이터만 다르다.
 *
 * 게시글과 달리 basePath가 없다: 뉴스는 상세 페이지가 없고 카드가 원문 링크로 바로 나간다.
 * 카테고리도 조회하지 않는다 — 응답이 이미 라벨 문자열로 온다(백엔드가 cms에서 해석).
 */
export default function NewsSearchResults({ keyword, section }: Props) {
  const [page, setPage] = useState(0)
  const [sort, setSort] = useState<SearchSort>('RELEVANCE')

  const { data, isLoading, isError, error } = useNewsSearchQuery(section, keyword, page, sort)

  if (isError) {
    // 검색 설정 없음(503)·조회 한계 초과(400) 등 — 서버 메시지를 그대로 노출한다
    return <Alert className={styles.alert} type="error" showIcon message={getErrorMessage(error)} />
  }

  if (isLoading) {
    return <Skeleton className={styles.skeleton} active paragraph={{ rows: 6 }} />
  }

  const news = data?.data ?? []
  const total = data?.totalCount ?? 0

  if (total === 0) {
    return (
      <div className={styles.empty}>
        <Empty description={`'${keyword}'에 대한 뉴스가 없습니다.`} />
      </div>
    )
  }

  return (
    <div>
      <div className={styles.toolbar}>
        <span className={styles.count}>{`총 ${total.toLocaleString()}건`}</span>
        <SearchResultSort
          value={sort}
          onChange={(next) => {
            setSort(next)
            // 정렬이 바뀌면 순서가 달라지므로 첫 페이지로 되돌린다
            setPage(0)
          }}
        />
      </div>

      <div className={styles.list}>
        {news.map((item) => (
          <NewsCard key={item.id} news={item} />
        ))}
      </div>

      <div className={styles.pagination}>
        <Pagination
          current={page + 1}
          pageSize={SEARCH_PAGE_SIZE}
          total={total}
          showSizeChanger={false}
          onChange={(next) => setPage(next - 1)}
        />
      </div>
    </div>
  )
}
