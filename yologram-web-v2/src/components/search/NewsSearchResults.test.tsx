import { describe, it, expect, beforeAll, afterEach, afterAll } from 'vitest'
import { screen, waitFor } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { http, HttpResponse } from 'msw'
import { server } from '@/test/server'
import { renderWithProviders } from '@/test/utils'
import NewsSearchResults from './NewsSearchResults'

const SEARCH_URL = 'http://localhost:5002/api/v2/search/tech/news'

beforeAll(() => server.listen())
afterEach(() => server.resetHandlers())
afterAll(() => server.close())

function news(id: number, title = `뉴스 ${id}`) {
  return {
    id,
    title,
    summary: '요약 본문',
    link: `https://news.test/${id}`,
    sourceName: 'GeekNews',
    categories: ['Backend'],
    publishedAt: '2026-08-10T16:23:47',
  }
}

/** 검색 응답 — 요청 파라미터를 캡처해 검증에 쓴다 */
function givenSearch(
  options: { total?: number; items?: ReturnType<typeof news>[]; capture?: (url: URL) => void } = {}
) {
  const items = options.items ?? [news(900)]
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
    })
  )
}

function renderResults(keyword = '마이그레이션') {
  return renderWithProviders(<NewsSearchResults keyword={keyword} section="tech" />)
}

describe('NewsSearchResults (결과 렌더)', () => {
  it('총 건수와 결과 목록을 렌더한다', async () => {
    givenSearch({ total: 28, items: [news(832, '모던 백엔드 - 마이그레이션')] })
    renderResults()

    expect(await screen.findByText('총 28건')).toBeInTheDocument()
    expect(screen.getByText('모던 백엔드 - 마이그레이션')).toBeInTheDocument()
  })

  it('카테고리 라벨은 응답 값을 그대로 쓴다', async () => {
    // 게시글과 달리 카테고리 API를 따로 부르지 않는다 — 백엔드가 라벨로 내려준다
    givenSearch()
    renderResults()

    expect(await screen.findByText('Backend')).toBeInTheDocument()
    expect(await screen.findByText('GeekNews')).toBeInTheDocument()
  })

  it('제목은 원문 링크로 나간다', async () => {
    // 뉴스는 상세 페이지가 없다 — 카드가 새 탭으로 원문을 연다
    givenSearch({ items: [news(900, '원문 제목')] })
    renderResults()

    const link = await screen.findByRole('link', { name: '원문 제목' })
    expect(link).toHaveAttribute('href', 'https://news.test/900')
    expect(link).toHaveAttribute('target', '_blank')
  })

  it('결과가 0건이면 빈 상태를 보여준다', async () => {
    givenSearch({ total: 0, items: [] })
    renderResults()

    expect(await screen.findByText("'마이그레이션'에 대한 뉴스가 없습니다.")).toBeInTheDocument()
  })
})

describe('NewsSearchResults (요청 파라미터)', () => {
  it('키워드·기본 페이지·정렬로 요청한다', async () => {
    let captured: URL | null = null
    givenSearch({ capture: (url) => (captured = url) })
    renderResults()

    await screen.findByText(/총 /)
    expect(captured!.searchParams.get('q')).toBe('마이그레이션')
    expect(captured!.searchParams.get('page')).toBe('0')
    expect(captured!.searchParams.get('size')).toBe('10')
    expect(captured!.searchParams.get('sort')).toBe('RELEVANCE')
  })

  it('페이지를 넘기면 page 파라미터가 바뀐다', async () => {
    const pages: string[] = []
    givenSearch({ total: 28, capture: (url) => pages.push(url.searchParams.get('page') ?? '') })
    const user = userEvent.setup()
    renderResults()

    await screen.findByText('총 28건')
    await user.click(screen.getByTitle('2'))

    await waitFor(() => expect(pages).toContain('1'))
  })

  it('정렬을 바꾸면 sort가 바뀌고 첫 페이지로 돌아간다', async () => {
    const calls: Array<{ sort: string | null; page: string | null }> = []
    givenSearch({
      total: 28,
      capture: (url) =>
        calls.push({ sort: url.searchParams.get('sort'), page: url.searchParams.get('page') }),
    })
    const user = userEvent.setup()
    renderResults()

    await screen.findByText('총 28건')
    await user.click(screen.getByTitle('2'))
    await waitFor(() => expect(calls.some((c) => c.page === '1')).toBe(true))

    await user.click(screen.getByText('최신순'))

    // 정렬이 바뀌면 순서가 달라지므로 1페이지로 되돌린다
    await waitFor(() => {
      expect(calls.some((c) => c.sort === 'LATEST' && c.page === '0')).toBe(true)
    })
  })
})

describe('NewsSearchResults (오류)', () => {
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
