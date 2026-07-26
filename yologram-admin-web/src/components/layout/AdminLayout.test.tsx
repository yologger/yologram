import { describe, it, expect, vi, beforeEach, beforeAll, afterEach, afterAll } from 'vitest'
import { screen, waitFor, within } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { Routes, Route } from 'react-router'
import { getDefaultStore } from 'jotai'
import { http, HttpResponse } from 'msw'
import { server } from '../../test/server'
import { renderWithProviders } from '../../test/utils'
import { authAtom, type AuthState } from '../../stores/auth'
import AdminLayout from './AdminLayout'

const mockUseIsMobile = vi.fn()
vi.mock('../../hooks/useIsMobile', () => ({
  default: () => mockUseIsMobile(),
}))

const adminAuth: AuthState = {
  uid: 1,
  accessToken: 'valid-token',
  email: 'admin@yologram.link',
  name: '관리자',
}

beforeAll(() => server.listen())
afterEach(() => {
  server.resetHandlers()
  getDefaultStore().set(authAtom, null)
  localStorage.removeItem('auth')
})
afterAll(() => server.close())

/** modal.confirm은 body portal로 렌더되고, 모바일에선 Drawer도 role="dialog"라 클래스로 특정한다. */
async function findConfirmModal() {
  await waitFor(() => {
    expect(document.querySelector('.ant-modal-confirm')).toBeInTheDocument()
  })
  return document.querySelector('.ant-modal-confirm') as HTMLElement
}

function renderLayout(initialPath = '/dashboard') {
  return renderWithProviders(
    <Routes>
      <Route element={<AdminLayout />}>
        <Route path="/dashboard" element={<div>dashboard content</div>} />
        <Route path="/ums/users" element={<div>users content</div>} />
        <Route path="/ums/admin-users" element={<div>admin-users content</div>} />
        <Route path="/categories" element={<div>categories content</div>} />
        <Route path="/posts" element={<div>posts content</div>} />
        <Route path="/news" element={<div>news content</div>} />
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
    expect(screen.getByText('유저 관리')).toBeInTheDocument()
    expect(screen.getByText('카테고리 관리')).toBeInTheDocument()
    expect(screen.getByText('게시글 관리')).toBeInTheDocument()
    expect(screen.getByText('뉴스 관리')).toBeInTheDocument()
  })

  it('현재 경로의 콘텐츠를 Outlet으로 렌더한다', () => {
    renderLayout('/dashboard')
    expect(screen.getByText('dashboard content')).toBeInTheDocument()
  })

  it('현재 경로의 메뉴가 선택 상태다', () => {
    renderLayout('/ums/users')
    expect(screen.getByText('유저 관리').closest('li')).toHaveClass('ant-menu-item-selected')
  })

  it('서브탭 하위 경로(/ums/admin-users)에서도 유저 관리 메뉴가 선택 상태다', () => {
    renderLayout('/ums/admin-users')
    expect(screen.getByText('유저 관리').closest('li')).toHaveClass('ant-menu-item-selected')
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
    expect(screen.queryByText('유저 관리')).not.toBeInTheDocument()
  })

  it('햄버거 클릭 시 Drawer가 열리고 전체 메뉴를 렌더한다', async () => {
    const user = userEvent.setup()
    renderLayout()
    await user.click(screen.getByLabelText('메뉴 열기'))
    expect(screen.getByText('대시보드')).toBeInTheDocument()
    expect(screen.getByText('유저 관리')).toBeInTheDocument()
    expect(screen.getByText('카테고리 관리')).toBeInTheDocument()
    expect(screen.getByText('게시글 관리')).toBeInTheDocument()
    expect(screen.getByText('뉴스 관리')).toBeInTheDocument()
  })

  it('Drawer에서 현재 경로의 메뉴가 선택 상태다', async () => {
    const user = userEvent.setup()
    renderLayout('/ums/users')
    await user.click(screen.getByLabelText('메뉴 열기'))
    expect(screen.getByText('유저 관리').closest('li')).toHaveClass('ant-menu-item-selected')
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
    renderLayout('/ums/users')
    expect(screen.getByText('users content')).toBeInTheDocument()
  })
})

describe('AdminLayout (어드민 정보·로그아웃, 데스크탑)', () => {
  beforeEach(() => {
    mockUseIsMobile.mockReturnValue(false)
    getDefaultStore().set(authAtom, adminAuth)
  })

  it('사이드바 하단에 로그인된 어드민 이름과 로그아웃 버튼을 렌더한다', () => {
    renderLayout()
    expect(screen.getByText('관리자')).toBeInTheDocument()
    expect(screen.getByRole('button', { name: /로그아웃/ })).toBeInTheDocument()
  })

  it('로그아웃 클릭 시 확인 모달이 뜨고, 확인하면 로그아웃 API 호출 후 인증 정보를 제거한다', async () => {
    let logoutCalled = false
    server.use(
      http.post('http://localhost:5001/api/v1/ums/admin/auth/logout', () => {
        logoutCalled = true
        return new HttpResponse(null, { status: 204 })
      }),
    )

    const user = userEvent.setup()
    renderLayout()

    await user.click(screen.getByRole('button', { name: /로그아웃/ }))

    const modal = await findConfirmModal()
    expect(within(modal).getByText('정말 로그아웃 하시겠어요?')).toBeInTheDocument()

    await user.click(within(modal).getByRole('button', { name: '로그아웃' }))

    await waitFor(() => {
      expect(logoutCalled).toBe(true)
    })
    await waitFor(() => {
      expect(localStorage.getItem('auth')).toBeNull()
    })
  })

  it('확인 모달에서 취소하면 인증 정보를 유지한다', async () => {
    const user = userEvent.setup()
    renderLayout()

    await user.click(screen.getByRole('button', { name: /로그아웃/ }))

    const modal = await findConfirmModal()
    await user.click(within(modal).getByRole('button', { name: '취소' }))

    expect(getDefaultStore().get(authAtom)).toEqual(adminAuth)
    expect(localStorage.getItem('auth')).not.toBeNull()
  })
})

describe('AdminLayout (어드민 정보·로그아웃, 모바일)', () => {
  beforeEach(() => {
    mockUseIsMobile.mockReturnValue(true)
    getDefaultStore().set(authAtom, adminAuth)
  })

  it('Drawer 하단에 로그인된 어드민 이름과 로그아웃 버튼을 렌더한다', async () => {
    const user = userEvent.setup()
    renderLayout()

    await user.click(screen.getByLabelText('메뉴 열기'))

    expect(screen.getByText('관리자')).toBeInTheDocument()
    expect(screen.getByRole('button', { name: /로그아웃/ })).toBeInTheDocument()
  })

  it('Drawer의 로그아웃 클릭 시 확인 모달이 뜨고, 확인하면 인증 정보를 제거한다', async () => {
    const user = userEvent.setup()
    renderLayout()

    await user.click(screen.getByLabelText('메뉴 열기'))
    await user.click(screen.getByRole('button', { name: /로그아웃/ }))

    const modal = await findConfirmModal()
    await user.click(within(modal).getByRole('button', { name: '로그아웃' }))

    await waitFor(() => {
      expect(localStorage.getItem('auth')).toBeNull()
    })
  })
})
