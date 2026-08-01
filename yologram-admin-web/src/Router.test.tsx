import { describe, it, expect, beforeAll, afterEach, afterAll } from 'vitest'
import { screen, waitFor } from '@testing-library/react'
import { getDefaultStore } from 'jotai'
import { server } from './test/server'
import { renderWithProviders } from './test/utils'
import { authAtom, type AuthState } from './stores/auth'
import Router from './Router'

const adminAuth: AuthState = {
  uid: 1,
  accessToken: 'valid-token',
  email: 'admin@yologram.link',
  name: '관리자',
  role: 'OWNER',
}

beforeAll(() => server.listen())
afterEach(() => {
  server.resetHandlers()
  getDefaultStore().set(authAtom, null)
  localStorage.removeItem('auth')
})
afterAll(() => server.close())

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

  it.each(['/', '/notices', '/ums', '/categories', '/posts', '/news'])(
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
  it('/ 진입 시 /notices로 리다이렉트한다', () => {
    renderRouterAuthenticated('/')
    expect(screen.getByRole('heading', { level: 3, name: '공지 관리' })).toBeInTheDocument()
  })

  it.each([
    ['/notices', '공지 관리'],
    ['/ums/users', '유저 관리'],
    ['/categories', '카테고리 관리'],
    ['/posts', '게시글 관리'],
    ['/news/tech', '기술 뉴스 관리'],
    ['/news/invest', '투자 뉴스 관리'],
    ['/news/politics', '정치 뉴스 관리'],
  ])('%s 진입 시 "%s" 준비 중 화면을 렌더한다', (path, title) => {
    renderRouterAuthenticated(path)
    expect(screen.getByRole('heading', { level: 3, name: title })).toBeInTheDocument()
    expect(screen.getByText('페이지 준비 중입니다')).toBeInTheDocument()
  })

  it('/ums 진입 시 /ums/users로 리다이렉트한다', () => {
    renderRouterAuthenticated('/ums')
    expect(screen.getByRole('heading', { level: 3, name: '유저 관리' })).toBeInTheDocument()
    expect(screen.getByText('페이지 준비 중입니다')).toBeInTheDocument()
  })

  it('/ums/admin-users 진입 시 어드민 관리 화면을 렌더한다', async () => {
    renderRouterAuthenticated('/ums/admin-users')
    expect(screen.getByRole('heading', { level: 3, name: '어드민 관리' })).toBeInTheDocument()
    expect(await screen.findByText('admin@yologram.link')).toBeInTheDocument()
  })

  it('/news/tech/sources 진입 시 소스 관리 화면을 렌더한다', async () => {
    renderRouterAuthenticated('/news/tech/sources')
    expect(screen.getByRole('heading', { level: 3, name: '소스 관리' })).toBeInTheDocument()
    expect(await screen.findByText('우아한형제들 기술블로그')).toBeInTheDocument()
  })

  it('/news 진입 시 /news/tech로 리다이렉트한다', () => {
    renderRouterAuthenticated('/news')
    expect(screen.getByRole('heading', { level: 3, name: '기술 뉴스 관리' })).toBeInTheDocument()
    expect(screen.getByText('페이지 준비 중입니다')).toBeInTheDocument()
  })

  it('알 수 없는 경로는 /notices로 리다이렉트한다', () => {
    renderRouterAuthenticated('/unknown-path')
    expect(screen.getByRole('heading', { level: 3, name: '공지 관리' })).toBeInTheDocument()
  })
})
