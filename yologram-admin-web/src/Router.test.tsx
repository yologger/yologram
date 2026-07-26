import { describe, it, expect, afterEach } from 'vitest'
import { screen, waitFor } from '@testing-library/react'
import { getDefaultStore } from 'jotai'
import { renderWithProviders } from './test/utils'
import { authAtom, type AuthState } from './stores/auth'
import Router from './Router'

const adminAuth: AuthState = {
  uid: 1,
  accessToken: 'valid-token',
  email: 'admin@yologram.link',
  name: '관리자',
}

afterEach(() => {
  getDefaultStore().set(authAtom, null)
  localStorage.removeItem('auth')
})

function renderRouter(initialPath: string) {
  return renderWithProviders(<Router />, {
    wrapperOptions: { routerProps: { initialEntries: [initialPath] } },
  })
}

function renderRouterAuthenticated(initialPath: string) {
  getDefaultStore().set(authAtom, adminAuth)
  return renderRouter(initialPath)
}

describe('Router (미인증)', () => {
  it('/login 진입 시 로그인 페이지를 렌더한다', () => {
    renderRouter('/login')
    expect(screen.getByPlaceholderText('이메일')).toBeInTheDocument()
    expect(screen.getByRole('button', { name: '로그인' })).toBeInTheDocument()
  })

  it.each(['/', '/dashboard', '/ums', '/categories', '/posts', '/news'])(
    '미인증으로 %s 진입 시 로그인 페이지로 리다이렉트한다',
    async (path) => {
      renderRouter(path)
      await waitFor(() => {
        expect(screen.getByPlaceholderText('이메일')).toBeInTheDocument()
      })
    },
  )
})

describe('Router (인증)', () => {
  it('/ 진입 시 /dashboard로 리다이렉트한다', () => {
    renderRouterAuthenticated('/')
    expect(screen.getByRole('heading', { level: 3, name: '대시보드' })).toBeInTheDocument()
  })

  it.each([
    ['/dashboard', '대시보드'],
    ['/categories', '카테고리 관리'],
    ['/posts', '게시글 관리'],
    ['/news', '뉴스 관리'],
  ])('%s 진입 시 "%s" 준비 중 화면을 렌더한다', (path, title) => {
    renderRouterAuthenticated(path)
    expect(screen.getByRole('heading', { level: 3, name: title })).toBeInTheDocument()
    expect(screen.getByText('페이지 준비 중입니다')).toBeInTheDocument()
  })

  it('/ums 진입 시 /ums/users로 리다이렉트하고 서브탭 골격을 렌더한다', async () => {
    renderRouterAuthenticated('/ums')
    expect(screen.getByRole('heading', { level: 3, name: '유저 관리' })).toBeInTheDocument()
    await waitFor(() => {
      expect(screen.getByRole('tab', { name: '유저 관리' })).toHaveAttribute('aria-selected', 'true')
    })
    expect(screen.getByRole('tab', { name: '어드민 관리' })).toBeInTheDocument()
    expect(screen.getByText('페이지 준비 중입니다')).toBeInTheDocument()
  })

  it('/ums/admin-users 진입 시 어드민 관리 탭이 선택된다', () => {
    renderRouterAuthenticated('/ums/admin-users')
    expect(screen.getByRole('tab', { name: '어드민 관리' })).toHaveAttribute('aria-selected', 'true')
    expect(screen.getByText('페이지 준비 중입니다')).toBeInTheDocument()
  })

  it('알 수 없는 경로는 /dashboard로 리다이렉트한다', () => {
    renderRouterAuthenticated('/unknown-path')
    expect(screen.getByRole('heading', { level: 3, name: '대시보드' })).toBeInTheDocument()
  })
})
