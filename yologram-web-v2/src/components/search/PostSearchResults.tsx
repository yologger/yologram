'use client'

import { useMemo, useState } from 'react'
import { useRouter } from 'next/navigation'
import { Alert, Empty, Pagination, Skeleton } from 'antd'
import PostCard from '@/components/community/PostCard'
import usePostSearchQuery, { SEARCH_PAGE_SIZE } from '@/queries/usePostSearchQuery'
import usePostCategoriesQuery from '@/queries/usePostCategoriesQuery'
import { getErrorMessage } from '@/lib/error'
import SearchResultSort from './SearchResultSort'
import type { SearchSort } from '@/apis/search'
import styles from './PostSearchResults.module.css'

interface Props {
  keyword: string
  /** 섹션 기본 경로 (예: "/tech") — 상세 이동에 쓴다 */
  basePath: string
  /** 검색 대상 섹션 (예: "tech") */
  section: string
}

/**
 * 커뮤니티(게시글) 검색 결과 — 목록 + 페이지 네비게이션.
 *
 * 무한 스크롤이 아니라 페이지 번호를 쓴다: 검색은 총 건수가 정보이고(커서로는 만들 수 없다),
 * 결과에서 상세로 들어갔다 돌아올 때 위치가 유지돼야 한다. 피드의 무한 스크롤과 의도적으로 다르다.
 */
export default function PostSearchResults({ keyword, basePath, section }: Props) {
  const router = useRouter()
  const [page, setPage] = useState(0)
  const [sort, setSort] = useState<SearchSort>('RELEVANCE')

  const { data, isLoading, isError, error } = usePostSearchQuery(section, keyword, page, sort)
  const { data: categories = [] } = usePostCategoriesQuery(section)
  const nameById = useMemo(() => new Map(categories.map((c) => [c.id, c.name])), [categories])

  if (isError) {
    // 검색 설정 없음(503)·조회 한계 초과(400) 등 — 서버 메시지를 그대로 노출한다
    return <Alert className={styles.alert} type="error" showIcon message={getErrorMessage(error)} />
  }

  if (isLoading) {
    return <Skeleton className={styles.skeleton} active paragraph={{ rows: 6 }} />
  }

  const posts = data?.data ?? []
  const total = data?.totalCount ?? 0

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
            categoryNames={post.categoryIds
              .map((id) => nameById.get(id))
              .filter((n): n is string => !!n)}
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
