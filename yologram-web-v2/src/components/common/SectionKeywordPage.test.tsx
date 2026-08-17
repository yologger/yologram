import { describe, it, expect, vi, beforeAll, beforeEach, afterEach, afterAll } from 'vitest'
import { screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { http, HttpResponse } from 'msw'
import { server } from '@/test/server'
import { renderWithProviders } from '../../test/utils'
import SectionKeywordPage from './SectionKeywordPage'

const NEWS_SEARCH_URL = 'http://localhost:5002/api/v2/search/tech/news'

const mockPush = vi.fn()
const mockUseParams = vi.fn()
vi.mock('next/navigation', () => ({
  useRouter: () => ({ push: mockPush }),
  useParams: () => mockUseParams(),
  usePathname: () => '/tech/keywords/test',
}))

function setViewport(width: number) {
  Object.defineProperty(window, 'innerWidth', { value: width, writable: true, configurable: true })
}

beforeAll(() => server.listen())
beforeEach(() => {
  mockPush.mockClear()
  setViewport(1024)
})
afterEach(() => server.resetHandlers())
afterAll(() => server.close())

describe('SectionKeywordPage', () => {
  it('인코딩된 한글 키워드를 디코딩해 검색결과 텍스트를 렌더링한다', () => {
    mockUseParams.mockReturnValue({ keyword: encodeURIComponent('제미나이') })
    renderWithProviders(<SectionKeywordPage basePath="/tech" />)

    expect(screen.getByText("'제미나이' 검색결과")).toBeInTheDocument()
  })

  it('검색바에 키워드가 초기값으로 채워진다', () => {
    mockUseParams.mockReturnValue({ keyword: encodeURIComponent('제미나이') })
    renderWithProviders(<SectionKeywordPage basePath="/tech" />)

    expect(screen.getByPlaceholderText('검색어를 입력하세요')).toHaveValue('제미나이')
  })

  it('이미 디코딩된 키워드도 그대로 렌더링한다', () => {
    mockUseParams.mockReturnValue({ keyword: '제미나이' })
    renderWithProviders(<SectionKeywordPage basePath="/tech" />)

    expect(screen.getByText("'제미나이' 검색결과")).toBeInTheDocument()
  })

  it('디코딩 불가한 키워드(%)는 원본 그대로 렌더링한다', () => {
    mockUseParams.mockReturnValue({ keyword: '100%' })
    renderWithProviders(<SectionKeywordPage basePath="/tech" />)

    expect(screen.getByText("'100%' 검색결과")).toBeInTheDocument()
  })

  it('커뮤니티·뉴스 탭을 렌더한다', () => {
    mockUseParams.mockReturnValue({ keyword: encodeURIComponent('제미나이') })
    renderWithProviders(<SectionKeywordPage basePath="/tech" />)

    expect(screen.getByRole('tab', { name: '커뮤니티' })).toBeInTheDocument()
    expect(screen.getByRole('tab', { name: '뉴스' })).toBeInTheDocument()
  })

  it('뉴스 탭은 뉴스 검색 결과를 보여준다', async () => {
    server.use(
      http.get(NEWS_SEARCH_URL, () =>
        HttpResponse.json({
          data: [
            {
              id: 900,
              title: '제미나이 3 공개',
              summary: '요약',
              link: 'https://news.test/900',
              sourceName: 'GeekNews',
              categories: ['AI'],
              publishedAt: '2026-08-10T16:23:47',
            },
          ],
          page: 0,
          size: 10,
          totalPages: 1,
          totalCount: 1,
          first: true,
          last: true,
        })
      )
    )
    mockUseParams.mockReturnValue({ keyword: encodeURIComponent('제미나이') })
    const user = userEvent.setup()
    renderWithProviders(<SectionKeywordPage basePath="/tech" />)

    await user.click(screen.getByRole('tab', { name: '뉴스' }))

    // 커뮤니티 탭과 별개로 자기 검색·페이징을 갖는다
    expect(await screen.findByText('제미나이 3 공개')).toBeInTheDocument()
    expect(await screen.findByText('총 1건')).toBeInTheDocument()
  })
})
