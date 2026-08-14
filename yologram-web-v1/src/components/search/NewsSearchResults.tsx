import { useState } from 'react'
import { Empty, Pagination } from 'antd'
import NewsCard from '../news/NewsCard'
import SearchResultSort, { type SearchSort } from './SearchResultSort'
import { SEARCH_PAGE_SIZE, MOCK_NEWS_TOTAL, mockNews } from './searchResultMock'
import styles from './SearchResults.module.css'

interface Props {
  keyword: string
}

/**
 * 뉴스 검색 결과 — 게시글 탭과 같은 형태 (web-v2 미러).
 *
 * 게시글과 한 목록에 섞지 않는 이유: 스키마가 다르고(author·metrics vs source·publishedAt),
 * _score는 인덱스별 IDF로 계산돼 서로 비교하면 문서 수가 적은 쪽이 유리해진다.
 *
 * TODO 뉴스 인덱싱·검색 엔드포인트 연동 — 지금은 목 데이터다.
 * GET /api/v1/search/tech/news?q&page&size&sort
 */
export default function NewsSearchResults({ keyword }: Props) {
  const [page, setPage] = useState(0)
  const [sort, setSort] = useState<SearchSort>('RELEVANCE')

  const news = mockNews(page)
  const total = MOCK_NEWS_TOTAL

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
