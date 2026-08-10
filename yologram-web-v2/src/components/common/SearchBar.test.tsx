import { describe, it, expect, vi, beforeEach } from 'vitest'
import { render, screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import SearchBar from './SearchBar'

const mockPush = vi.fn()
vi.mock('next/navigation', () => ({
  useRouter: () => ({ push: mockPush }),
}))

// useIsMobile은 window.innerWidth로 판별(768 미만이 모바일)
function setViewport(width: number) {
  Object.defineProperty(window, 'innerWidth', { value: width, writable: true, configurable: true })
}

beforeEach(() => {
  mockPush.mockClear()
  setViewport(1024)
})

describe('SearchBar (데스크탑)', () => {
  it('placeholder와 초기값을 렌더링한다', () => {
    render(<SearchBar basePath="/tech" initialValue="제미나이" />)

    const input = screen.getByPlaceholderText('검색어를 입력하세요')
    expect(input).toHaveValue('제미나이')
  })

  it('Enter 시 인코딩된 키워드 경로로 이동한다', async () => {
    const user = userEvent.setup()
    render(<SearchBar basePath="/tech" />)

    await user.type(screen.getByPlaceholderText('검색어를 입력하세요'), 'ai{Enter}')
    expect(mockPush).toHaveBeenCalledWith('/tech/keywords/ai')
  })

  it('한글 키워드는 encodeURIComponent로 인코딩한다', async () => {
    const user = userEvent.setup()
    render(<SearchBar basePath="/tech" />)

    await user.type(screen.getByPlaceholderText('검색어를 입력하세요'), '제미나이{Enter}')
    expect(mockPush).toHaveBeenCalledWith(`/tech/keywords/${encodeURIComponent('제미나이')}`)
  })

  it('앞뒤 공백은 trim 후 이동한다', async () => {
    const user = userEvent.setup()
    render(<SearchBar basePath="/tech" />)

    await user.type(screen.getByPlaceholderText('검색어를 입력하세요'), '  ai  {Enter}')
    expect(mockPush).toHaveBeenCalledWith('/tech/keywords/ai')
  })

  it('빈 값이면 Enter를 무시한다', async () => {
    const user = userEvent.setup()
    render(<SearchBar basePath="/tech" />)

    await user.type(screen.getByPlaceholderText('검색어를 입력하세요'), '{Enter}')
    expect(mockPush).not.toHaveBeenCalled()
  })

  it('공백만 입력하면 Enter를 무시한다', async () => {
    const user = userEvent.setup()
    render(<SearchBar basePath="/tech" />)

    await user.type(screen.getByPlaceholderText('검색어를 입력하세요'), '   {Enter}')
    expect(mockPush).not.toHaveBeenCalled()
  })

  it('basePath에 따라 이동 경로가 달라진다', async () => {
    const user = userEvent.setup()
    render(<SearchBar basePath="/invest" />)

    await user.type(screen.getByPlaceholderText('검색어를 입력하세요'), '금리{Enter}')
    expect(mockPush).toHaveBeenCalledWith(`/invest/keywords/${encodeURIComponent('금리')}`)
  })
})

describe('SearchBar (모바일)', () => {
  beforeEach(() => setViewport(500))

  it('인라인 검색바 대신 돋보기 버튼만 보인다', () => {
    render(<SearchBar basePath="/tech" />)

    expect(screen.getByRole('button', { name: '검색' })).toBeInTheDocument()
    expect(screen.queryByPlaceholderText('검색어를 입력하세요')).not.toBeInTheDocument()
  })

  it('돋보기 탭 시 오버레이가 열리고 입력창에 autofocus된다', async () => {
    const user = userEvent.setup()
    render(<SearchBar basePath="/tech" />)

    await user.click(screen.getByRole('button', { name: '검색' }))
    const input = screen.getByPlaceholderText('검색어를 입력하세요')
    expect(input).toHaveFocus()
  })

  it('뒤로 버튼으로 오버레이가 닫힌다', async () => {
    const user = userEvent.setup()
    render(<SearchBar basePath="/tech" />)

    await user.click(screen.getByRole('button', { name: '검색' }))
    await user.click(screen.getByRole('button', { name: '뒤로' }))
    expect(screen.queryByPlaceholderText('검색어를 입력하세요')).not.toBeInTheDocument()
  })

  it('ESC로 오버레이가 닫힌다', async () => {
    const user = userEvent.setup()
    render(<SearchBar basePath="/tech" />)

    await user.click(screen.getByRole('button', { name: '검색' }))
    await user.keyboard('{Escape}')
    expect(screen.queryByPlaceholderText('검색어를 입력하세요')).not.toBeInTheDocument()
  })

  it('오버레이에서 Enter 시 인코딩 경로로 이동하고 오버레이가 닫힌다', async () => {
    const user = userEvent.setup()
    render(<SearchBar basePath="/tech" />)

    await user.click(screen.getByRole('button', { name: '검색' }))
    await user.type(screen.getByPlaceholderText('검색어를 입력하세요'), '제미나이{Enter}')

    expect(mockPush).toHaveBeenCalledWith(`/tech/keywords/${encodeURIComponent('제미나이')}`)
    expect(screen.queryByPlaceholderText('검색어를 입력하세요')).not.toBeInTheDocument()
  })

  it('오버레이에서 빈 값 Enter는 무시하고 오버레이를 유지한다', async () => {
    const user = userEvent.setup()
    render(<SearchBar basePath="/tech" />)

    await user.click(screen.getByRole('button', { name: '검색' }))
    await user.type(screen.getByPlaceholderText('검색어를 입력하세요'), '{Enter}')

    expect(mockPush).not.toHaveBeenCalled()
    expect(screen.getByPlaceholderText('검색어를 입력하세요')).toBeInTheDocument()
  })
})
