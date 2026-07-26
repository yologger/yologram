import { describe, it, expect, vi, beforeEach } from 'vitest'
import { screen, waitFor } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { Routes, Route } from 'react-router'
import { renderWithProviders } from '../../test/utils'
import AdminLayout from './AdminLayout'

const mockUseIsMobile = vi.fn()
vi.mock('../../hooks/useIsMobile', () => ({
  default: () => mockUseIsMobile(),
}))

function renderLayout(initialPath = '/dashboard') {
  return renderWithProviders(
    <Routes>
      <Route element={<AdminLayout />}>
        <Route path="/dashboard" element={<div>dashboard content</div>} />
        <Route path="/users" element={<div>users content</div>} />
        <Route path="/categories" element={<div>categories content</div>} />
        <Route path="/posts" element={<div>posts content</div>} />
        <Route path="/feeds" element={<div>feeds content</div>} />
      </Route>
    </Routes>,
    { wrapperOptions: { routerProps: { initialEntries: [initialPath] } } },
  )
}

describe('AdminLayout (데스크탑)', () => {
  beforeEach(() => {
    mockUseIsMobile.mockReturnValue(false)
  })

  it('사이드바에 어드민 타이틀과 전체 메뉴를 렌더한다', () => {
    renderLayout()
    expect(screen.getByText('yologram admin')).toBeInTheDocument()
    expect(screen.getByText('대시보드')).toBeInTheDocument()
    expect(screen.getByText('회원 관리')).toBeInTheDocument()
    expect(screen.getByText('카테고리 관리')).toBeInTheDocument()
    expect(screen.getByText('게시글 관리')).toBeInTheDocument()
    expect(screen.getByText('RSS 피드 관리')).toBeInTheDocument()
  })

  it('현재 경로의 콘텐츠를 Outlet으로 렌더한다', () => {
    renderLayout('/dashboard')
    expect(screen.getByText('dashboard content')).toBeInTheDocument()
  })

  it('현재 경로의 메뉴가 선택 상태다', () => {
    renderLayout('/users')
    expect(screen.getByText('회원 관리').closest('li')).toHaveClass('ant-menu-item-selected')
  })

  it('메뉴 클릭 시 해당 경로로 이동한다', async () => {
    const user = userEvent.setup()
    renderLayout('/dashboard')
    await user.click(screen.getByText('게시글 관리'))
    expect(screen.getByText('posts content')).toBeInTheDocument()
  })

  it('모바일 헤더(햄버거 버튼)는 렌더하지 않는다', () => {
    renderLayout()
    expect(screen.queryByLabelText('메뉴 열기')).not.toBeInTheDocument()
  })

  it('매칭되는 메뉴가 없는 경로에서는 선택된 메뉴가 없다', () => {
    renderWithProviders(
      <Routes>
        <Route element={<AdminLayout />}>
          <Route path="/unknown" element={<div>unknown content</div>} />
        </Route>
      </Routes>,
      { wrapperOptions: { routerProps: { initialEntries: ['/unknown'] } } },
    )
    expect(document.querySelector('.ant-menu-item-selected')).not.toBeInTheDocument()
  })
})

describe('AdminLayout (모바일)', () => {
  beforeEach(() => {
    mockUseIsMobile.mockReturnValue(true)
  })

  it('상단 헤더에 햄버거 버튼과 타이틀을 렌더한다', () => {
    renderLayout()
    expect(screen.getByLabelText('메뉴 열기')).toBeInTheDocument()
    expect(screen.getByText('yologram admin')).toBeInTheDocument()
  })

  it('고정 사이드바는 렌더하지 않고 Drawer도 닫혀 있다', () => {
    renderLayout()
    expect(document.querySelector('.ant-layout-sider')).not.toBeInTheDocument()
    expect(screen.queryByText('회원 관리')).not.toBeInTheDocument()
  })

  it('햄버거 클릭 시 Drawer가 열리고 전체 메뉴를 렌더한다', async () => {
    const user = userEvent.setup()
    renderLayout()
    await user.click(screen.getByLabelText('메뉴 열기'))
    expect(screen.getByText('대시보드')).toBeInTheDocument()
    expect(screen.getByText('회원 관리')).toBeInTheDocument()
    expect(screen.getByText('카테고리 관리')).toBeInTheDocument()
    expect(screen.getByText('게시글 관리')).toBeInTheDocument()
    expect(screen.getByText('RSS 피드 관리')).toBeInTheDocument()
  })

  it('Drawer에서 현재 경로의 메뉴가 선택 상태다', async () => {
    const user = userEvent.setup()
    renderLayout('/users')
    await user.click(screen.getByLabelText('메뉴 열기'))
    expect(screen.getByText('회원 관리').closest('li')).toHaveClass('ant-menu-item-selected')
  })

  it('Drawer 메뉴 클릭 시 해당 경로로 이동하고 Drawer가 닫힌다', async () => {
    const user = userEvent.setup()
    renderLayout('/dashboard')
    await user.click(screen.getByLabelText('메뉴 열기'))
    await user.click(screen.getByText('게시글 관리'))
    expect(screen.getByText('posts content')).toBeInTheDocument()
    await waitFor(() => {
      expect(document.querySelector('.ant-drawer-open')).not.toBeInTheDocument()
    })
  })

  it('현재 경로의 콘텐츠를 렌더한다', () => {
    renderLayout('/users')
    expect(screen.getByText('users content')).toBeInTheDocument()
  })
})
