import { describe, it, expect, vi, beforeEach } from 'vitest'
import { render, screen } from '@testing-library/react'
import SubTabLayout from './SubTabLayout'

const mockPush = vi.fn()
const mockUsePathname = vi.fn()
vi.mock('next/navigation', () => ({
  useRouter: () => ({ push: mockPush }),
  usePathname: () => mockUsePathname(),
}))

function setViewport(width: number) {
  Object.defineProperty(window, 'innerWidth', { value: width, writable: true, configurable: true })
}

const tabs = [
  { key: 'news', label: '뉴스' },
  { key: 'community', label: '커뮤니티' },
]

beforeEach(() => {
  mockPush.mockClear()
  mockUsePathname.mockReturnValue('/tech/news')
  setViewport(1024)
})

describe('SubTabLayout', () => {
  it('타이틀·검색바·탭·children을 렌더링한다', () => {
    render(
      <SubTabLayout basePath="/tech" tabs={tabs} title="기술">
        <div>본문</div>
      </SubTabLayout>,
    )

    expect(screen.getByRole('heading', { name: '기술' })).toBeInTheDocument()
    expect(screen.getByPlaceholderText('검색어를 입력하세요')).toBeInTheDocument()
    expect(screen.getByRole('tab', { name: '뉴스' })).toBeInTheDocument()
    expect(screen.getByText('본문')).toBeInTheDocument()
  })

  it('검색바가 타이틀과 탭 사이(DOM 순서)에 렌더링된다', () => {
    render(
      <SubTabLayout basePath="/tech" tabs={tabs} title="기술">
        <div>본문</div>
      </SubTabLayout>,
    )

    const title = screen.getByRole('heading', { name: '기술' })
    const search = screen.getByPlaceholderText('검색어를 입력하세요')
    const tablist = screen.getByRole('tablist')

    // DOCUMENT_POSITION_FOLLOWING: 인자가 기준 노드보다 뒤에 위치
    expect(title.compareDocumentPosition(search) & Node.DOCUMENT_POSITION_FOLLOWING).toBeTruthy()
    expect(search.compareDocumentPosition(tablist) & Node.DOCUMENT_POSITION_FOLLOWING).toBeTruthy()
  })

  it('모바일에서는 인라인 검색바 대신 돋보기 버튼이 보인다', () => {
    setViewport(500)
    render(
      <SubTabLayout basePath="/tech" tabs={tabs} title="기술">
        <div>본문</div>
      </SubTabLayout>,
    )

    expect(screen.getByRole('button', { name: '검색' })).toBeInTheDocument()
    expect(screen.queryByPlaceholderText('검색어를 입력하세요')).not.toBeInTheDocument()
  })
})
