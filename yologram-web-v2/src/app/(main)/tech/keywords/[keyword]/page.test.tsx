import { describe, it, expect, vi, beforeEach } from 'vitest'
import { render, screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import TechKeywordPage from './page'

const mockPush = vi.fn()
const mockUseParams = vi.fn()
vi.mock('next/navigation', () => ({
  useRouter: () => ({ push: mockPush }),
  useParams: () => mockUseParams(),
}))

beforeEach(() => {
  mockPush.mockClear()
  Object.defineProperty(window, 'innerWidth', { value: 1024, writable: true, configurable: true })
  mockUseParams.mockReturnValue({ keyword: encodeURIComponent('제미나이') })
})

describe('TechKeywordPage', () => {
  it('디코딩된 키워드로 검색결과 텍스트를 렌더링한다', () => {
    render(<TechKeywordPage />)

    expect(screen.getByText("'제미나이' 검색결과")).toBeInTheDocument()
  })

  it('검색바에서 재검색 시 /tech 키워드 경로로 이동한다', async () => {
    const user = userEvent.setup()
    render(<TechKeywordPage />)

    const input = screen.getByPlaceholderText('검색어를 입력하세요')
    await user.clear(input)
    await user.type(input, '쿠버네티스{Enter}')

    expect(mockPush).toHaveBeenCalledWith(`/tech/keywords/${encodeURIComponent('쿠버네티스')}`)
  })
})
