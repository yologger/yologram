'use client'

import { useState } from 'react'
import { useRouter } from 'next/navigation'
import { Empty, Pagination } from 'antd'
import PostCard from '@/components/community/PostCard'
import SearchResultSort, { type SearchSort } from './SearchResultSort'
import { SEARCH_PAGE_SIZE, MOCK_POST_TOTAL, mockPosts } from './searchResultMock'
import styles from './PostSearchResults.module.css'

interface Props {
  keyword: string
  basePath: string
}

/**
 * 커뮤니티(게시글) 검색 결과 — 목록 + 페이지 네비게이션.
 *
 * 무한 스크롤이 아니라 페이지 번호를 쓴다: 검색은 총 건수가 정보이고(커서로는 만들 수 없다),
 * 결과에서 상세로 들어갔다 돌아올 때 위치가 유지돼야 한다. 목록·뉴스 탭의 무한 스크롤과 의도적으로 다르다.
 *
 * TODO 검색 엔드포인트 연동 — 지금은 목 데이터다.
 * GET /api/v1/search/tech/posts?q&page&size&sort → data·totalCount·totalPages
 */
export default function PostSearchResults({ keyword, basePath }: Props) {
  const router = useRouter()
  const [page, setPage] = useState(0)
  const [sort, setSort] = useState<SearchSort>('RELEVANCE')

  const posts = mockPosts(page)
  const total = MOCK_POST_TOTAL

  if (total === 0) {
    return (
      <div className={styles.empty}>
        <Empty description={`'${keyword}'에 대한 게시글이 없습니다.`} />
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
        {posts.map((post) => (
          <PostCard
            key={post.id}
            post={post}
            onClick={() => router.push(`${basePath}/community/${post.id}`)}
          />
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
