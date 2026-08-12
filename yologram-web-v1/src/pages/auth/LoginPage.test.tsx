import { describe, it, expect, beforeAll, afterAll, afterEach, vi } from 'vitest'
import { screen, waitFor } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { server } from '../../test/server'
import { renderWithProviders } from '../../test/utils'
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
        expect(mockNavigate).toHaveBeenCalledWith('/')
      })
    })

    it('returnTo state가 있으면 성공 시 해당 경로로 복귀한다', async () => {
      const user = userEvent.setup()
      // 로그인 유도 모달에서 넘어온 상황 재현 — location.state.returnTo 전달
      renderWithProviders(<LoginPage />, {
        wrapperOptions: {
          routerProps: {
            initialEntries: [{ pathname: '/login', state: { returnTo: '/tech/community/1' } }],
          },
        },
      })

      await user.type(screen.getByPlaceholderText('이메일'), 'test@yologram.link')
      await user.type(screen.getByPlaceholderText('비밀번호'), 'password123!')
      await user.click(screen.getByRole('button', { name: '로그인' }))

      await waitFor(() => {
        expect(mockNavigate).toHaveBeenCalledWith('/tech/community/1')
      })
    })
  })

  describe('로그인 실패', () => {
    it('존재하지 않는 사용자일 때 에러 메시지를 표시한다', async () => {
      const user = userEvent.setup()
      renderWithProviders(<LoginPage />)

      await user.type(screen.getByPlaceholderText('이메일'), 'notfound@yologram.link')
      await user.type(screen.getByPlaceholderText('비밀번호'), 'password123!')
      await user.click(screen.getByRole('button', { name: '로그인' }))

      await waitFor(() => {
        expect(screen.getByText('존재하지 않는 사용자입니다.')).toBeInTheDocument()
      })
    })

    it('비밀번호 불일치 시 에러 메시지를 표시한다', async () => {
      const user = userEvent.setup()
      renderWithProviders(<LoginPage />)

      await user.type(screen.getByPlaceholderText('이메일'), 'test@yologram.link')
      await user.type(screen.getByPlaceholderText('비밀번호'), 'wrongpassword')
      await user.click(screen.getByRole('button', { name: '로그인' }))

      await waitFor(() => {
        expect(screen.getByText('비밀번호가 일치하지 않습니다.')).toBeInTheDocument()
      })
    })
  })
})
