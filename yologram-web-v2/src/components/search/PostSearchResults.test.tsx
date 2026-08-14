import { describe, it, expect, vi, beforeAll, afterEach, afterAll } from 'vitest'
import { screen, waitFor, within } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { http, HttpResponse } from 'msw'
import { server } from '@/test/server'
import { renderWithProviders } from '@/test/utils'
import PostSearchResults from './PostSearchResults'

const mockPush = vi.fn()
vi.mock('next/navigation', () => ({
  useRouter: () => ({ push: mockPush }),
  usePathname: () => '/tech/keywords/test',
}))

const SEARCH_URL = 'http://localhost:5002/api/v2/search/tech/posts'
const CATEGORIES_URL = 'http://localhost:5002/api/v2/cms/tech/categories'

beforeAll(() => server.listen())
afterEach(() => {
  server.resetHandlers()
  mockPush.mockClear()
})
afterAll(() => server.close())

function post(id: number, title = `제목 ${id}`) {
  return {
    id,
    section: 'TECH',
    author: { uid: 12, nickname: 'tester0' },
    title,
    content: '본문',
    categoryIds: [1],
    metrics: { commentCount: 0, likeCount: 0, viewCount: 0, likedByMe: false },
    createdAt: '2026-07-18T14:23:50',
  }
}

/** 검색 응답 — 요청 파라미터를 캡처해 검증에 쓴다 */
function givenSearch(
  options: { total?: number; items?: ReturnType<typeof post>[]; capture?: (url: URL) => void } = {}
) {
  const items = options.items ?? [post(1200)]
  const total = options.total ?? items.length
  server.use(
    http.get(SEARCH_URL, ({ request }) => {
      const url = new URL(request.url)
      options.capture?.(url)
      const size = Number(url.searchParams.get('size') ?? 10)
      return HttpResponse.json({
        data: items,
        page: Number(url.searchParams.get('page') ?? 0),
        size,
        totalPages: Math.ceil(total / size),
        totalCount: total,
        first: true,
        last: false,
      })
    }),
    http.get(CATEGORIES_URL, () => HttpResponse.json({ data: [{ id: 1, name: '백엔드' }] }))
  )
}

function renderResults(keyword = '제미나이') {
  return renderWithProviders(
    <PostSearchResults keyword={keyword} basePath="/tech" section="tech" />
  )
}

describe('PostSearchResults (결과 렌더)', () => {
  it('총 건수와 결과 목록을 렌더한다', async () => {
    givenSearch({ total: 44, items: [post(1200, 'OpenSearch 색인 설계')] })
    renderResults()

    expect(await screen.findByText('총 44건')).toBeInTheDocument()
    expect(screen.getByText('OpenSearch 색인 설계')).toBeInTheDocument()
  })

  it('카테고리 이름을 카드에 함께 넘긴다', async () => {
    givenSearch()
    renderResults()

    // 색인 문서에는 categoryIds만 있어 이름은 카테고리 API에서 매핑한다
    expect(await screen.findByText('백엔드')).toBeInTheDocument()
  })

  it('결과가 0건이면 빈 상태를 보여준다', async () => {
    givenSearch({ total: 0, items: [] })
    renderResults()

    expect(await screen.findByText("'제미나이'에 대한 게시글이 없습니다.")).toBeInTheDocument()
  })

  it('항목을 클릭하면 상세로 이동한다', async () => {
    givenSearch({ items: [post(1200)] })
    const user = userEvent.setup()
    renderResults()

    await user.click(await screen.findByText('제목 1200'))

    expect(mockPush).toHaveBeenCalledWith('/tech/community/1200')
  })
})

describe('PostSearchResults (요청 파라미터)', () => {
  it('키워드·기본 페이지·정렬로 요청한다', async () => {
    let captured: URL | null = null
    givenSearch({ capture: (url) => (captured = url) })
    renderResults()

    await screen.findByText(/총 /)
    expect(captured!.searchParams.get('q')).toBe('제미나이')
    expect(captured!.searchParams.get('page')).toBe('0')
    expect(captured!.searchParams.get('size')).toBe('10')
    expect(captured!.searchParams.get('sort')).toBe('RELEVANCE')
  })

  it('페이지를 넘기면 page 파라미터가 바뀐다', async () => {
    const pages: string[] = []
    givenSearch({ total: 44, capture: (url) => pages.push(url.searchParams.get('page') ?? '') })
    const user = userEvent.setup()
    renderResults()

    await screen.findByText('총 44건')
    await user.click(screen.getByTitle('2'))

    await waitFor(() => expect(pages).toContain('1'))
  })

  it('정렬을 바꾸면 sort가 바뀌고 첫 페이지로 돌아간다', async () => {
    const calls: Array<{ sort: string | null; page: string | null }> = []
    givenSearch({
      total: 44,
      capture: (url) =>
        calls.push({ sort: url.searchParams.get('sort'), page: url.searchParams.get('page') }),
    })
    const user = userEvent.setup()
    renderResults()

    await screen.findByText('총 44건')
    await user.click(screen.getByTitle('2'))
    await waitFor(() => expect(calls.some((c) => c.page === '1')).toBe(true))

    await user.click(screen.getByText('최신순'))

    // 정렬이 바뀌면 순서가 달라지므로 1페이지로 되돌린다
    await waitFor(() => {
      expect(calls.some((c) => c.sort === 'LATEST' && c.page === '0')).toBe(true)
    })
  })
})

describe('PostSearchResults (오류)', () => {
  it('검색 설정이 없으면(503) 서버 메시지를 보여준다', async () => {
    server.use(
      http.get(SEARCH_URL, () =>
        HttpResponse.json(
          { errorMessage: '검색 기능을 사용할 수 없습니다.', errorCode: 'SEARCH_UNAVAILABLE' },
          { status: 503 }
        )
      )
    )
    renderResults()

    expect(await screen.findByText('검색 기능을 사용할 수 없습니다.')).toBeInTheDocument()
  })

  it('조회 한계를 넘으면(400) 서버 메시지를 보여준다', async () => {
    server.use(
      http.get(SEARCH_URL, () =>
        HttpResponse.json(
          { errorMessage: '더 이상 조회할 수 없는 페이지입니다.', errorCode: 'SEARCH_PAGE_TOO_DEEP' },
          { status: 400 }
        )
      )
    )
    renderResults()

    expect(await screen.findByText('더 이상 조회할 수 없는 페이지입니다.')).toBeInTheDocument()
  })
})
