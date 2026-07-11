import { describe, it, expect } from 'vitest'
import { screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { Routes, Route } from 'react-router'
import { renderWithProviders } from '../../test/utils'
import AdminLayout from './AdminLayout'

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

describe('AdminLayout', () => {
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
