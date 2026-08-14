import type { PostSummary } from '@/apis/pms'
import type { News } from '@/apis/news'

/**
 * 검색 결과 목 데이터 — UI 확인용 임시.
 * 검색 엔드포인트(/api/v1/search/tech/{posts,news})가 나오면 이 파일을 지우고
 * usePostSearchQuery·useNewsSearchQuery로 교체한다.
 */

/** 한 페이지 크기 — 실제 API의 size 파라미터와 같은 값을 쓴다 */
export const SEARCH_PAGE_SIZE = 10

// number로 명시 — 리터럴로 좁혀지면 빈 결과 분기(total === 0)가 타입 에러가 된다
export const MOCK_POST_TOTAL: number = 34
export const MOCK_NEWS_TOTAL: number = 57

const POST_TITLES = [
  'Elasticsearch 색인 설계 노트',
  'OpenSearch nori 분석기 적용 후기',
  'SQS 기반 비동기 인덱싱 파이프라인',
  'Kinesis로 조회수 집계하기',
  'ECS Fargate 비용 최적화 기록',
  'Spring Boot 3.5 마이그레이션',
  'QueryDSL 커서 페이징 구현',
  'Valkey 캐시 도입과 무효화 전략',
  'JPA 배치 INSERT 튜닝',
  'Terraform 모노레포 전환',
]

const NEWS_SOURCES = ['GeekNews', '우아한형제들 기술블로그', '카카오 기술블로그', '토스 기술블로그']

export function mockPosts(page: number): PostSummary[] {
  const start = page * SEARCH_PAGE_SIZE
  const count = Math.min(SEARCH_PAGE_SIZE, MOCK_POST_TOTAL - start)
  return Array.from({ length: Math.max(0, count) }, (_, i) => {
    const n = start + i
    return {
      id: 1200 - n,
      section: 'TECH',
      author: { uid: 12, nickname: `tester${n % 5}` },
      title: POST_TITLES[n % POST_TITLES.length],
      content:
        '검색 결과 본문 미리보기입니다. 실제 연동 시 색인된 content가 이 자리에 들어갑니다. ' +
        '길면 카드에서 잘립니다.',
      categoryIds: [(n % 3) + 1],
      metrics: {
        commentCount: n % 7,
        likeCount: n % 11,
        viewCount: 10 + n * 3,
        likedByMe: false,
      },
      createdAt: new Date(Date.UTC(2026, 6, 18, 14, 23, 50) - n * 3600_000).toISOString().slice(0, 19),
    }
  })
}

export function mockNews(page: number): News[] {
  const start = page * SEARCH_PAGE_SIZE
  const count = Math.min(SEARCH_PAGE_SIZE, MOCK_NEWS_TOTAL - start)
  return Array.from({ length: Math.max(0, count) }, (_, i) => {
    const n = start + i
    return {
      id: 900 - n,
      title: `${POST_TITLES[n % POST_TITLES.length]} — 뉴스 제목 예시`,
      summary:
        'LLM이 요약한 뉴스 본문이 이 자리에 들어갑니다. 검색 결과에서는 요약을 그대로 보여줍니다.',
      link: 'https://news.hada.io/',
      sourceName: NEWS_SOURCES[n % NEWS_SOURCES.length],
      categories: ['백엔드'],
      publishedAt: new Date(Date.UTC(2026, 7, 14, 17, 0, 0) - n * 5400_000).toISOString().slice(0, 19),
    }
  })
}
