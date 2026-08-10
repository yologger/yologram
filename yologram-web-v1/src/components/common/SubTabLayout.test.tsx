import { describe, it, expect, vi, beforeEach } from 'vitest'
import { screen } from '@testing-library/react'
import { Routes, Route } from 'react-router'
import { renderWithProviders } from '../../test/utils'
import SubTabLayout from './SubTabLayout'

let mockIsMobile = false
vi.mock('../../hooks/useIsMobile', () => ({ default: () => mockIsMobile }))

beforeEach(() => {
  mockIsMobile = false
})

const tabs = [
  { key: 'news', label: '뉴스' },
  { key: 'community', label: '커뮤니티' },
]

function renderLayout() {
  return renderWithProviders(
    <Routes>
      <Route path="/tech" element={<SubTabLayout basePath="/tech" tabs={tabs} title="기술" />}>
        <Route path="news" element={<div>뉴스 내용</div>} />
      </Route>
    </Routes>,
    { wrapperOptions: { routerProps: { initialEntries: ['/tech/news'] } } },
  )
}

describe('SubTabLayout 검색바', () => {
  it('데스크탑에서 검색바가 타이틀과 탭 사이에 렌더된다', () => {
    renderLayout()
    const title = screen.getByText('기술')
    const search = screen.getByPlaceholderText('검색어를 입력하세요')
    const tablist = screen.getByRole('tablist')
    // 타이틀 → 검색바 → 탭 순서 확인
    expect(title.compareDocumentPosition(search) & Node.DOCUMENT_POSITION_FOLLOWING).toBeTruthy()
    expect(search.compareDocumentPosition(tablist) & Node.DOCUMENT_POSITION_FOLLOWING).toBeTruthy()
  })

  it('모바일에서는 인라인 검색바 대신 돋보기 아이콘 버튼이 렌더된다', () => {
    mockIsMobile = true
    renderLayout()
    expect(screen.queryByPlaceholderText('검색어를 입력하세요')).not.toBeInTheDocument()
    expect(screen.getByRole('button', { name: '검색' })).toBeInTheDocument()
  })

  it('타이틀·탭·자식 컨텐츠가 함께 렌더된다', () => {
    renderLayout()
    expect(screen.getByText('기술')).toBeInTheDocument()
    expect(screen.getByText('뉴스')).toBeInTheDocument()
    expect(screen.getByText('뉴스 내용')).toBeInTheDocument()
  })
})
