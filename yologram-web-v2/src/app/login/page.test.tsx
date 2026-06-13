import { describe, it, expect, beforeAll, afterAll, afterEach, vi } from 'vitest'
import { screen, waitFor } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { server } from '../../test/server'
import { renderWithProviders } from '../../test/utils'
import LoginPage from './page'

const mockPush = vi.fn()
vi.mock('next/navigation', () => ({
  useRouter: () => ({ push: mockPush }),
}))

beforeAll(() => server.listen())
afterEach(() => {
  server.resetHandlers()
  mockPush.mockClear()
})
afterAll(() => server.close())

describe('LoginPage', () => {
  describe('렌더링', () => {
    it('로그인 폼이 렌더링된다', () => {
      renderWithProviders(<LoginPage />)

      expect(screen.getByPlaceholderText('이메일')).toBeInTheDocument()
      expect(screen.getByPlaceholderText('비밀번호')).toBeInTheDocument()
      expect(screen.getByRole('button', { name: '로그인' })).toBeInTheDocument()
      expect(screen.getByText('회원가입')).toBeInTheDocument()
    })

    it('테스트 힌트가 표시되지 않는다', () => {
      renderWithProviders(<LoginPage />)

      expect(screen.queryByText(/테스트:/)).not.toBeInTheDocument()
    })
  })

  describe('입력값 검증', () => {
    it('필수 필드가 비어있으면 로그인 버튼이 비활성화된다', () => {
      renderWithProviders(<LoginPage />)

      expect(screen.getByRole('button', { name: '로그인' })).toBeDisabled()
    })

    it('모든 필드가 채워지면 로그인 버튼이 활성화된다', async () => {
      const user = userEvent.setup()
      renderWithProviders(<LoginPage />)

      await user.type(screen.getByPlaceholderText('이메일'), 'test@yologram.link')
      await user.type(screen.getByPlaceholderText('비밀번호'), 'password123!')

      await waitFor(() => {
        expect(screen.getByRole('button', { name: '로그인' })).toBeEnabled()
      })
    })
  })

  describe('로그인 성공', () => {
    it('성공 시 메인 페이지로 이동한다', async () => {
      const user = userEvent.setup()
      renderWithProviders(<LoginPage />)

      await user.type(screen.getByPlaceholderText('이메일'), 'test@yologram.link')
      await user.type(screen.getByPlaceholderText('비밀번호'), 'password123!')
      await user.click(screen.getByRole('button', { name: '로그인' }))

      await waitFor(() => {
        expect(mockPush).toHaveBeenCalledWith('/')
      })
    })
  })

  describe('로그인 실패', () => {
    it('존재하지 않는 사용자 시 에러 메시지를 표시한다', async () => {
      const user = userEvent.setup()
      renderWithProviders(<LoginPage />)

      await user.type(screen.getByPlaceholderText('이메일'), 'notfound@yologram.link')
      await user.type(screen.getByPlaceholderText('비밀번호'), 'password123!')
      await user.click(screen.getByRole('button', { name: '로그인' }))

      await waitFor(() => {
        expect(screen.getByText('사용자를 찾을 수 없습니다.')).toBeInTheDocument()
      })
    })
  })
})
