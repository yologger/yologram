import { describe, it, expect, afterEach } from 'vitest'
import { screen } from '@testing-library/react'
import { Routes, Route, useLocation } from 'react-router'
import { getDefaultStore } from 'jotai'
import { renderWithProviders } from '../../test/utils'
import { authAtom } from '../../stores/auth'
import RequireAuth from './RequireAuth'

// 로그인 페이지 대역 — 전달받은 location.state.returnTo를 노출해 검증
function LoginProbe() {
  const location = useLocation()
  const returnTo = (location.state as { returnTo?: string } | null)?.returnTo
  return <div>로그인 페이지 returnTo:{returnTo ?? '없음'}</div>
}

const login = () =>
  getDefaultStore().set(authAtom, {
    uid: 1,
    accessToken: 'valid-token',
    email: 'test@yologram.link',
    name: '테스터',
    nickname: 'tester',
  })

afterEach(() => {
  getDefaultStore().set(authAtom, null)
})

const renderAt = (initialEntry: string) =>
  renderWithProviders(
    <Routes>
      <Route path="/login" element={<LoginProbe />} />
      <Route element={<RequireAuth />}>
        <Route path="/tech/community/write" element={<div>글쓰기 페이지</div>} />
      </Route>
    </Routes>,
    { wrapperOptions: { routerProps: { initialEntries: [initialEntry] } } },
  )

describe('RequireAuth', () => {
  it('비로그인 상태로 보호 라우트에 직접 진입하면 returnTo와 함께 로그인 페이지로 이동한다', async () => {
    renderAt('/tech/community/write')

    // 로그인 후 원래 목적지로 복귀할 수 있게 진입 경로를 returnTo로 전달
    expect(await screen.findByText('로그인 페이지 returnTo:/tech/community/write')).toBeInTheDocument()
    expect(screen.queryByText('글쓰기 페이지')).not.toBeInTheDocument()
  })

  it('쿼리스트링이 있으면 returnTo에 함께 담는다', async () => {
    renderAt('/tech/community/write?draft=1')

    expect(
      await screen.findByText('로그인 페이지 returnTo:/tech/community/write?draft=1'),
    ).toBeInTheDocument()
  })

  it('로그인 상태면 보호 라우트를 그대로 렌더링한다', async () => {
    login()
    renderAt('/tech/community/write')

    expect(await screen.findByText('글쓰기 페이지')).toBeInTheDocument()
    expect(screen.queryByText(/로그인 페이지/)).not.toBeInTheDocument()
  })
})
