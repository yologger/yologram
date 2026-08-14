import { describe, it, expect, vi, beforeEach } from 'vitest'
import { screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { renderWithProviders } from '../../test/utils'
import SectionKeywordPage from './SectionKeywordPage'

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

beforeEach(() => {
  mockPush.mockClear()
  setViewport(1024)
})

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

  it('뉴스 탭은 준비 중 안내를 보여준다', async () => {
    // 뉴스는 아직 색인이 없다 (todos — tech-news-index 신설이 선행)
    mockUseParams.mockReturnValue({ keyword: encodeURIComponent('제미나이') })
    const user = userEvent.setup()
    renderWithProviders(<SectionKeywordPage basePath="/tech" />)

    await user.click(screen.getByRole('tab', { name: '뉴스' }))

    expect(await screen.findByText('페이지 준비 중입니다')).toBeInTheDocument()
  })
})
