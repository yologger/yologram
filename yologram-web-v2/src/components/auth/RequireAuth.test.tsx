import { describe, it, expect, vi, afterEach } from 'vitest'
import { render, screen, waitFor } from '@testing-library/react'
import { getDefaultStore } from 'jotai'
import { authAtom } from '@/stores/auth'
import RequireAuth from './RequireAuth'

const mockReplace = vi.fn()
vi.mock('next/navigation', () => ({
  useRouter: () => ({ replace: mockReplace }),
  usePathname: () => '/settings/my-posts',
}))

const store = getDefaultStore()

afterEach(() => {
  store.set(authAtom, null)
  mockReplace.mockClear()
})

describe('RequireAuth', () => {
  it('비로그인 진입 시 현재 경로를 returnTo 쿼리로 담아 로그인 페이지로 이동한다', async () => {
    // 비로그인 상태 유지(afterEach에서 null 설정)
    render(
      <RequireAuth>
        <div>보호 콘텐츠</div>
      </RequireAuth>,
    )

    await waitFor(() =>
      expect(mockReplace).toHaveBeenCalledWith(`/login?returnTo=${encodeURIComponent('/settings/my-posts')}`),
    )
    // 리다이렉트 전까지 보호 콘텐츠는 렌더되지 않음
    expect(screen.queryByText('보호 콘텐츠')).not.toBeInTheDocument()
  })

  it('로그인 상태면 이동 없이 자식을 렌더링한다', async () => {
    store.set(authAtom, { uid: 1, email: 't@yologram.link', name: '테스터', nickname: 'tester', accessToken: 't' })
    render(
      <RequireAuth>
        <div>보호 콘텐츠</div>
      </RequireAuth>,
    )

    expect(await screen.findByText('보호 콘텐츠')).toBeInTheDocument()
    expect(mockReplace).not.toHaveBeenCalled()
  })
})
