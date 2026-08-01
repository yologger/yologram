import { describe, it, expect, beforeAll, afterAll, afterEach, vi } from 'vitest'
import { screen, waitFor } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { getDefaultStore } from 'jotai'
import { server } from '../../test/server'
import { renderWithProviders } from '../../test/utils'
import { authAtom } from '../../stores/auth'
import LoginPage from './LoginPage'

const mockNavigate = vi.fn()
vi.mock('react-router', async () => {
  const actual = await vi.importActual('react-router')
  return { ...actual, useNavigate: () => mockNavigate }
})

beforeAll(() => server.listen())
afterEach(() => {
  server.resetHandlers()
  mockNavigate.mockClear()
  getDefaultStore().set(authAtom, null)
  localStorage.removeItem('auth')
})
afterAll(() => server.close())

describe('LoginPage', () => {
  describe('렌더링', () => {
    it('로그인 폼이 렌더링된다', () => {
      renderWithProviders(<LoginPage />)

      expect(screen.getByPlaceholderText('이메일')).toBeInTheDocument()
      expect(screen.getByPlaceholderText('비밀번호')).toBeInTheDocument()
      expect(screen.getByRole('button', { name: '로그인' })).toBeInTheDocument()
    })

    it('회원가입·비밀번호 찾기 링크는 렌더링하지 않는다', () => {
      renderWithProviders(<LoginPage />)

      expect(screen.queryByText('회원가입')).not.toBeInTheDocument()
      expect(screen.queryByText(/비밀번호를 잊으셨나요/)).not.toBeInTheDocument()
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

      await user.type(screen.getByPlaceholderText('이메일'), 'admin@yologram.link')
      await user.type(screen.getByPlaceholderText('비밀번호'), 'password123!')

      await waitFor(() => {
        expect(screen.getByRole('button', { name: '로그인' })).toBeEnabled()
      })
    })
  })

  describe('로그인 성공', () => {
    it('성공 시 인증 상태를 저장하고 공지 관리로 이동한다', async () => {
      const user = userEvent.setup()
      renderWithProviders(<LoginPage />)

      await user.type(screen.getByPlaceholderText('이메일'), 'admin@yologram.link')
      await user.type(screen.getByPlaceholderText('비밀번호'), 'password123!')
      await user.click(screen.getByRole('button', { name: '로그인' }))

      await waitFor(() => {
        expect(mockNavigate).toHaveBeenCalledWith('/notices')
      })
      expect(getDefaultStore().get(authAtom)).toEqual({
        uid: 1,
        accessToken: 'mock-access-token',
        email: 'admin@yologram.link',
        name: '관리자',
        role: 'OWNER',
      })
    })
  })

  describe('로그인 실패', () => {
    it('존재하지 않는 어드민일 때 에러 메시지를 표시한다', async () => {
      const user = userEvent.setup()
      renderWithProviders(<LoginPage />)

      await user.type(screen.getByPlaceholderText('이메일'), 'notfound@yologram.link')
      await user.type(screen.getByPlaceholderText('비밀번호'), 'password123!')
      await user.click(screen.getByRole('button', { name: '로그인' }))

      await waitFor(() => {
        expect(screen.getByText('존재하지 않는 어드민입니다.')).toBeInTheDocument()
      })
      expect(mockNavigate).not.toHaveBeenCalled()
    })

    it('비활성화된 계정이면 에러 메시지를 표시한다', async () => {
      const user = userEvent.setup()
      renderWithProviders(<LoginPage />)

      await user.type(screen.getByPlaceholderText('이메일'), 'inactive@yologram.link')
      await user.type(screen.getByPlaceholderText('비밀번호'), 'password123!')
      await user.click(screen.getByRole('button', { name: '로그인' }))

      await waitFor(() => {
        expect(screen.getByText('비활성화된 계정입니다.')).toBeInTheDocument()
      })
      expect(mockNavigate).not.toHaveBeenCalled()
    })

    it('비밀번호 불일치 시 에러 메시지를 표시한다', async () => {
      const user = userEvent.setup()
      renderWithProviders(<LoginPage />)

      await user.type(screen.getByPlaceholderText('이메일'), 'admin@yologram.link')
      await user.type(screen.getByPlaceholderText('비밀번호'), 'wrongpassword')
      await user.click(screen.getByRole('button', { name: '로그인' }))

      await waitFor(() => {
        expect(screen.getByText('비밀번호가 일치하지 않습니다.')).toBeInTheDocument()
      })
      expect(mockNavigate).not.toHaveBeenCalled()
    })
  })
})
