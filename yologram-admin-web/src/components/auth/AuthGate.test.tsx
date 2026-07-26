import { describe, it, expect, beforeAll, afterAll, afterEach } from 'vitest'
import { screen, waitFor } from '@testing-library/react'
import { getDefaultStore } from 'jotai'
import { server } from '../../test/server'
import { renderWithProviders } from '../../test/utils'
import Router from '../../Router'
import { authAtom, type AuthState } from '../../stores/auth'
import AuthGate from './AuthGate'

const persistedAuth: AuthState = {
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

describe('AuthGate', () => {
  it('저장된 토큰이 유효하면 보호 라우트를 유지한다', async () => {
    getDefaultStore().set(authAtom, persistedAuth)

    renderWithProviders(
      <AuthGate>
        <Router />
      </AuthGate>,
      {
        wrapperOptions: {
          routerProps: { initialEntries: ['/dashboard'] },
        },
      },
    )

    expect(screen.getByRole('status', { name: '인증 확인 중' })).toBeInTheDocument()

    await waitFor(() => {
      expect(screen.getByRole('heading', { level: 3, name: '대시보드' })).toBeInTheDocument()
    })
    expect(screen.getByText('관리자')).toBeInTheDocument()
  })

  it('저장된 토큰이 유효하지 않으면 인증 상태를 비우고 로그인 페이지로 보낸다', async () => {
    getDefaultStore().set(authAtom, {
      ...persistedAuth,
      accessToken: 'expired-token',
    })

    renderWithProviders(
      <AuthGate>
        <Router />
      </AuthGate>,
      {
        wrapperOptions: {
          routerProps: { initialEntries: ['/dashboard'] },
        },
      },
    )

    await waitFor(() => {
      expect(getDefaultStore().get(authAtom)).toBeNull()
    })

    await waitFor(() => {
      expect(screen.getByPlaceholderText('이메일')).toBeInTheDocument()
    })
  })

  it('저장된 토큰이 없으면 검증 없이 바로 렌더한다', async () => {
    renderWithProviders(
      <AuthGate>
        <Router />
      </AuthGate>,
      {
        wrapperOptions: {
          routerProps: { initialEntries: ['/dashboard'] },
        },
      },
    )

    await waitFor(() => {
      expect(screen.getByPlaceholderText('이메일')).toBeInTheDocument()
    })
  })
})
