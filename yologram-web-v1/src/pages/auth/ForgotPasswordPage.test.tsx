import { describe, it, expect, beforeAll, afterAll, afterEach, vi } from 'vitest'
import { screen, waitFor } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { server } from '../../test/server'
import { renderWithProviders } from '../../test/utils'
import ForgotPasswordPage from './ForgotPasswordPage'

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

async function reachPasswordStep(user: ReturnType<typeof userEvent.setup>, email = 'test@yologram.link') {
  await user.type(screen.getByPlaceholderText('이메일'), email)
  await user.click(screen.getByRole('button', { name: '코드 발송' }))

  const codeInput = await screen.findByPlaceholderText('인증 코드 6자리')
  await user.type(codeInput, '123456')
  await user.click(screen.getByRole('button', { name: '인증 확인' }))

  await waitFor(() => {
    expect(screen.getByText('코드 확인 완료')).toBeInTheDocument()
  })
}

describe('ForgotPasswordPage', () => {
  describe('렌더링', () => {
    it('이메일 입력과 코드 발송 버튼이 보인다', () => {
      renderWithProviders(<ForgotPasswordPage />)

      expect(screen.getByPlaceholderText('이메일')).toBeInTheDocument()
      expect(screen.getByRole('button', { name: '코드 발송' })).toBeInTheDocument()
      expect(screen.getByText('로그인으로 돌아가기')).toBeInTheDocument()
    })

    it('이메일이 유효하지 않으면 코드 발송 버튼이 비활성화된다', () => {
      renderWithProviders(<ForgotPasswordPage />)

      expect(screen.getByRole('button', { name: '코드 발송' })).toBeDisabled()
    })
  })

  describe('코드 발송', () => {
    it('가입되지 않은 이메일이면 에러 메시지를 표시한다', async () => {
      const user = userEvent.setup()
      renderWithProviders(<ForgotPasswordPage />)

      await user.type(screen.getByPlaceholderText('이메일'), 'notfound@yologram.link')
      await user.click(screen.getByRole('button', { name: '코드 발송' }))

      await waitFor(() => {
        expect(screen.getByText('사용자를 찾을 수 없습니다.')).toBeInTheDocument()
      })
    })

    it('발송 성공 시 코드 입력란이 노출된다', async () => {
      const user = userEvent.setup()
      renderWithProviders(<ForgotPasswordPage />)

      await user.type(screen.getByPlaceholderText('이메일'), 'test@yologram.link')
      await user.click(screen.getByRole('button', { name: '코드 발송' }))

      expect(await screen.findByPlaceholderText('인증 코드 6자리')).toBeInTheDocument()
    })
  })

  describe('코드 검증', () => {
    it('잘못된 코드면 에러 메시지를 표시한다', async () => {
      const user = userEvent.setup()
      renderWithProviders(<ForgotPasswordPage />)

      await user.type(screen.getByPlaceholderText('이메일'), 'test@yologram.link')
      await user.click(screen.getByRole('button', { name: '코드 발송' }))

      const codeInput = await screen.findByPlaceholderText('인증 코드 6자리')
      await user.type(codeInput, '000000')
      await user.click(screen.getByRole('button', { name: '인증 확인' }))

      await waitFor(() => {
        expect(screen.getByText('인증 코드가 일치하지 않습니다.')).toBeInTheDocument()
      })
    })

    it('검증 성공 시 새 비밀번호 입력란이 노출된다', async () => {
      const user = userEvent.setup()
      renderWithProviders(<ForgotPasswordPage />)

      await reachPasswordStep(user)

      expect(screen.getByPlaceholderText('새 비밀번호')).toBeInTheDocument()
      expect(screen.getByPlaceholderText('새 비밀번호 확인')).toBeInTheDocument()
    })
  })

  describe('조기 제출 방지', () => {
    it('이메일만 입력하고 Enter 시 재설정 요청이 가지 않는다', async () => {
      const user = userEvent.setup()
      renderWithProviders(<ForgotPasswordPage />)

      const emailInput = screen.getByPlaceholderText('이메일')
      await user.type(emailInput, 'test@yologram.link{enter}')

      await waitFor(() => {
        expect(mockNavigate).not.toHaveBeenCalled()
      })
    })
  })

  describe('비밀번호 변경', () => {
    it('새 비밀번호 불일치 시 변경 버튼이 비활성화된다', async () => {
      const user = userEvent.setup()
      renderWithProviders(<ForgotPasswordPage />)

      await reachPasswordStep(user)

      await user.type(screen.getByPlaceholderText('새 비밀번호'), 'newpass1234')
      await user.type(screen.getByPlaceholderText('새 비밀번호 확인'), 'different123')

      await waitFor(() => {
        expect(screen.getByRole('button', { name: '비밀번호 변경' })).toBeDisabled()
      })
    })

    it('성공 시 로그인 페이지로 이동한다', async () => {
      const user = userEvent.setup()
      renderWithProviders(<ForgotPasswordPage />)

      await reachPasswordStep(user)

      await user.type(screen.getByPlaceholderText('새 비밀번호'), 'newpass1234')
      await user.type(screen.getByPlaceholderText('새 비밀번호 확인'), 'newpass1234')
      await user.click(screen.getByRole('button', { name: '비밀번호 변경' }))

      await waitFor(() => {
        expect(mockNavigate).toHaveBeenCalledWith('/login')
      })
    })
  })
})
