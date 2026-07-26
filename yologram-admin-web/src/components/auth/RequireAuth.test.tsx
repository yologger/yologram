import { describe, it, expect, afterEach } from 'vitest'
import { screen, waitFor } from '@testing-library/react'
import { Routes, Route } from 'react-router'
import { getDefaultStore } from 'jotai'
import { renderWithProviders } from '../../test/utils'
import { authAtom, type AuthState } from '../../stores/auth'
import RequireAuth from './RequireAuth'

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

function renderGuarded(initialPath = '/protected') {
  return renderWithProviders(
    <Routes>
      <Route path="/login" element={<div>login page</div>} />
      <Route element={<RequireAuth />}>
        <Route path="/protected" element={<div>protected content</div>} />
      </Route>
    </Routes>,
    { wrapperOptions: { routerProps: { initialEntries: [initialPath] } } },
  )
}

describe('RequireAuth', () => {
  it('미인증이면 보호 콘텐츠를 렌더하지 않고 /login으로 리다이렉트한다', async () => {
    renderGuarded()

    expect(screen.queryByText('protected content')).not.toBeInTheDocument()

    await waitFor(() => {
      expect(screen.getByText('login page')).toBeInTheDocument()
    })
  })

  it('인증 상태면 보호 콘텐츠를 렌더한다', () => {
    getDefaultStore().set(authAtom, adminAuth)

    renderGuarded()

    expect(screen.getByText('protected content')).toBeInTheDocument()
    expect(screen.queryByText('login page')).not.toBeInTheDocument()
  })
})
