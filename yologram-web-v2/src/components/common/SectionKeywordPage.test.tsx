import { describe, it, expect, vi, beforeEach } from 'vitest'
import { render, screen } from '@testing-library/react'
import SectionKeywordPage from './SectionKeywordPage'

const mockPush = vi.fn()
const mockUseParams = vi.fn()
vi.mock('next/navigation', () => ({
  useRouter: () => ({ push: mockPush }),
  useParams: () => mockUseParams(),
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
    render(<SectionKeywordPage basePath="/tech" />)

    expect(screen.getByText("'제미나이' 검색결과")).toBeInTheDocument()
  })

  it('검색바에 키워드가 초기값으로 채워진다', () => {
    mockUseParams.mockReturnValue({ keyword: encodeURIComponent('제미나이') })
    render(<SectionKeywordPage basePath="/tech" />)

    expect(screen.getByPlaceholderText('검색어를 입력하세요')).toHaveValue('제미나이')
  })

  it('이미 디코딩된 키워드도 그대로 렌더링한다', () => {
    mockUseParams.mockReturnValue({ keyword: '제미나이' })
    render(<SectionKeywordPage basePath="/tech" />)

    expect(screen.getByText("'제미나이' 검색결과")).toBeInTheDocument()
  })

  it('디코딩 불가한 키워드(%)는 원본 그대로 렌더링한다', () => {
    mockUseParams.mockReturnValue({ keyword: '100%' })
    render(<SectionKeywordPage basePath="/tech" />)

    expect(screen.getByText("'100%' 검색결과")).toBeInTheDocument()
  })
})
