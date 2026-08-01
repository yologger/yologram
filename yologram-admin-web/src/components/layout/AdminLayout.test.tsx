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

/** 데스크탑 상단 바(최상위 메뉴) 영역 */
function getHeader() {
  return document.querySelector('.ant-layout-header') as HTMLElement
}

/** 데스크탑 좌측 사이드바(하위 메뉴) 영역 */
function getSider() {
  return document.querySelector('.ant-layout-sider') as HTMLElement
}

/** modal.confirm은 body portal로 렌더되고, Drawer·Dropdown도 별도 portal이라 클래스로 특정한다. */
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
        <Route path="/news/tech" element={<div>news tech content</div>} />
        <Route path="/news/invest" element={<div>news invest content</div>} />
        <Route path="/news/politics" element={<div>news politics content</div>} />
        <Route path="/news/tech/sources" element={<div>news sources content</div>} />
      </Route>
    </Routes>,
    { wrapperOptions: { routerProps: { initialEntries: [initialPath] } } },
  )
}

describe('AdminLayout (데스크탑)', () => {
  beforeEach(() => {
    mockUseIsMobile.mockReturnValue(false)
  })

  it('상단 바에 로고와 최상위 메뉴 5개를 렌더한다', () => {
    renderLayout()
    const header = getHeader()
    expect(within(header).getByText('yologram admin')).toBeInTheDocument()
    expect(within(header).getByText('대시보드')).toBeInTheDocument()
    expect(within(header).getByText('유저 관리')).toBeInTheDocument()
    expect(within(header).getByText('카테고리 관리')).toBeInTheDocument()
    expect(within(header).getByText('게시글 관리')).toBeInTheDocument()
    expect(within(header).getByText('뉴스 관리')).toBeInTheDocument()
  })

  it('현재 경로가 속한 최상위 메뉴가 상단 바에서 선택 상태다', () => {
    renderLayout('/ums/admin-users')
    expect(within(getHeader()).getByText('유저 관리').closest('li')).toHaveClass('ant-menu-item-selected')
  })

  it('사이드바에는 현재 최상위 메뉴의 하위 분류만 렌더한다', () => {
    renderLayout('/ums/users')
    const sider = getSider()
    expect(within(sider).getByText('유저 관리')).toBeInTheDocument()
    expect(within(sider).getByText('어드민 관리')).toBeInTheDocument()
    expect(within(sider).queryByText('대시보드')).not.toBeInTheDocument()
    expect(within(sider).queryByText('기술 뉴스')).not.toBeInTheDocument()
  })

  it('단일 하위 분류 섹션(대시보드)은 사이드바에 해당 항목 하나만 렌더한다', () => {
    renderLayout('/dashboard')
    const sider = getSider()
    expect(within(sider).getByText('대시보드')).toBeInTheDocument()
    expect(sider.querySelectorAll('li.ant-menu-item')).toHaveLength(1)
  })

  it('현재 경로의 하위 메뉴가 사이드바에서 선택 상태다', () => {
    renderLayout('/ums/admin-users')
    expect(within(getSider()).getByText('어드민 관리').closest('li')).toHaveClass('ant-menu-item-selected')
  })

  it('상단 메뉴 클릭 시 해당 섹션의 첫 하위 경로로 이동하고 사이드바가 바뀐다', async () => {
    const user = userEvent.setup()
    renderLayout('/dashboard')
    await user.click(within(getHeader()).getByText('뉴스 관리'))
    expect(screen.getByText('news tech content')).toBeInTheDocument()
    const sider = getSider()
    expect(within(sider).getByText('기술 뉴스')).toBeInTheDocument()
    expect(within(sider).getByText('투자 뉴스')).toBeInTheDocument()
    expect(within(sider).getByText('정치 뉴스')).toBeInTheDocument()
    expect(within(sider).queryByText('대시보드')).not.toBeInTheDocument()
  })

  it('/news/tech/sources 경로에서 상단 뉴스 관리·사이드바 소스 관리가 선택 상태다', () => {
    renderLayout('/news/tech/sources')
    expect(within(getHeader()).getByText('뉴스 관리').closest('li')).toHaveClass('ant-menu-item-selected')
    // 기술 뉴스 SubMenu가 기본 펼침이라 하위 소스 관리가 보이고 선택 상태다
    expect(within(getSider()).getByText('소스 관리').closest('li')).toHaveClass('ant-menu-item-selected')
    // 가장 긴 프리픽스 매칭 — 상위 경로(/news/tech)의 뉴스 관리는 선택되지 않는다
    expect(within(getSider()).getByText('뉴스 관리').closest('li')).not.toHaveClass('ant-menu-item-selected')
    expect(screen.getByText('news sources content')).toBeInTheDocument()
  })

  it('/news/tech 경로에서 기술 뉴스 SubMenu가 펼쳐지고 뉴스 관리가 선택 상태다', () => {
    renderLayout('/news/tech')
    const sider = getSider()
    expect(within(sider).getByText('기술 뉴스')).toBeInTheDocument()
    expect(within(sider).getByText('뉴스 관리').closest('li')).toHaveClass('ant-menu-item-selected')
    expect(within(sider).getByText('소스 관리').closest('li')).not.toHaveClass('ant-menu-item-selected')
  })

  it('사이드바 하위 메뉴 클릭 시 해당 경로로 이동한다', async () => {
    const user = userEvent.setup()
    renderLayout('/ums/users')
    await user.click(within(getSider()).getByText('어드민 관리'))
    expect(screen.getByText('admin-users content')).toBeInTheDocument()
  })

  it('현재 경로의 콘텐츠를 Outlet으로 렌더한다', () => {
    renderLayout('/dashboard')
    expect(screen.getByText('dashboard content')).toBeInTheDocument()
  })

  it('모바일 헤더(햄버거 버튼)는 렌더하지 않는다', () => {
    renderLayout()
    expect(screen.queryByLabelText('메뉴 열기')).not.toBeInTheDocument()
  })

  it('매칭되는 메뉴가 없는 경로에서는 선택된 메뉴가 없고 사이드바가 비어 있다', () => {
    renderWithProviders(
      <Routes>
        <Route element={<AdminLayout />}>
          <Route path="/unknown" element={<div>unknown content</div>} />
        </Route>
      </Routes>,
      { wrapperOptions: { routerProps: { initialEntries: ['/unknown'] } } },
    )
    expect(document.querySelector('.ant-menu-item-selected')).not.toBeInTheDocument()
    expect(getSider().querySelectorAll('li.ant-menu-item')).toHaveLength(0)
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
    expect(screen.queryByText('어드민 관리')).not.toBeInTheDocument()
  })

  it('햄버거 클릭 시 Drawer가 열리고 최상위 그룹과 하위 메뉴를 2단으로 렌더한다', async () => {
    const user = userEvent.setup()
    renderLayout()
    await user.click(screen.getByLabelText('메뉴 열기'))
    // 그룹 라벨과 하위 항목 라벨이 같은 섹션은 두 번 렌더된다
    expect(screen.getAllByText('대시보드')).toHaveLength(2)
    expect(screen.getAllByText('유저 관리')).toHaveLength(2)
    expect(screen.getAllByText('카테고리 관리')).toHaveLength(2)
    expect(screen.getAllByText('게시글 관리')).toHaveLength(2)
    // 뉴스 관리는 그룹 라벨 + 기술 뉴스 SubMenu 하위 '뉴스 관리' 리프로 두 번
    expect(screen.getAllByText('뉴스 관리')).toHaveLength(2)
    expect(screen.getByText('어드민 관리')).toBeInTheDocument()
    // 기술 뉴스 SubMenu는 기본 펼침 — 하위 소스 관리까지 보인다
    expect(screen.getByText('기술 뉴스')).toBeInTheDocument()
    expect(screen.getByText('소스 관리')).toBeInTheDocument()
    expect(screen.getByText('투자 뉴스')).toBeInTheDocument()
    expect(screen.getByText('정치 뉴스')).toBeInTheDocument()
  })

  it('Drawer에서 현재 경로의 하위 메뉴가 선택 상태다', async () => {
    const user = userEvent.setup()
    renderLayout('/ums/admin-users')
    await user.click(screen.getByLabelText('메뉴 열기'))
    expect(screen.getByText('어드민 관리').closest('li')).toHaveClass('ant-menu-item-selected')
  })

  it('Drawer 하위 메뉴(SubMenu 내부 리프) 클릭 시 해당 경로로 이동하고 Drawer가 닫힌다', async () => {
    const user = userEvent.setup()
    renderLayout('/dashboard')
    await user.click(screen.getByLabelText('메뉴 열기'))
    await user.click(screen.getByText('소스 관리'))
    expect(screen.getByText('news sources content')).toBeInTheDocument()
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

  it('상단 바 우측에 로그인된 어드민 이름을 렌더하고, 클릭하면 로그아웃 항목이 열린다', async () => {
    const user = userEvent.setup()
    renderLayout()

    expect(within(getHeader()).getByText('관리자')).toBeInTheDocument()

    await user.click(screen.getByText('관리자'))

    expect(await screen.findByRole('menuitem', { name: /로그아웃/ })).toBeInTheDocument()
  })

  it('로그아웃 항목 클릭 시 확인 모달이 뜨고, 확인하면 로그아웃 API 호출 후 인증 정보를 제거한다', async () => {
    let logoutCalled = false
    server.use(
      http.post('http://localhost:5001/api/v1/ums/admin/auth/logout', () => {
        logoutCalled = true
        return new HttpResponse(null, { status: 204 })
      }),
    )

    const user = userEvent.setup()
    renderLayout()

    await user.click(screen.getByText('관리자'))
    await user.click(await screen.findByRole('menuitem', { name: /로그아웃/ }))

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

    await user.click(screen.getByText('관리자'))
    await user.click(await screen.findByRole('menuitem', { name: /로그아웃/ }))

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
