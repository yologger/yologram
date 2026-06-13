import { describe, it, expect, beforeAll, afterAll, afterEach, vi } from 'vitest'
import { screen, waitFor } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { server } from '../../test/server'
import { renderWithProviders } from '../../test/utils'
import JoinPage from './JoinPage'

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

async function verifyEmailStep(user: ReturnType<typeof userEvent.setup>, email = 'new@yologram.link') {
  await user.type(screen.getByPlaceholderText('이메일'), email)
  await user.click(screen.getByRole('button', { name: '인증코드 발송' }))

  const codeInput = await screen.findByPlaceholderText('인증 코드 6자리')
  await user.type(codeInput, '123456')
  await user.click(screen.getByRole('button', { name: '인증 확인' }))

  await waitFor(() => {
    expect(screen.getByText('이메일 인증 완료')).toBeInTheDocument()
  })
}

describe('JoinPage', () => {
  describe('렌더링', () => {
    it('회원가입 폼이 렌더링된다', () => {
      renderWithProviders(<JoinPage />)

      expect(screen.getByPlaceholderText('이메일')).toBeInTheDocument()
      expect(screen.getByRole('button', { name: '인증코드 발송' })).toBeInTheDocument()
      expect(screen.getByPlaceholderText('이름')).toBeInTheDocument()
      expect(screen.getByPlaceholderText('닉네임')).toBeInTheDocument()
      expect(screen.getByPlaceholderText('비밀번호')).toBeInTheDocument()
      expect(screen.getByRole('button', { name: '회원가입' })).toBeInTheDocument()
      expect(screen.getByText('로그인')).toBeInTheDocument()
    })

    it('인증 전에는 이름/닉네임/비밀번호와 회원가입 버튼이 비활성화된다', () => {
      renderWithProviders(<JoinPage />)

      expect(screen.getByPlaceholderText('이름')).toBeDisabled()
      expect(screen.getByPlaceholderText('닉네임')).toBeDisabled()
      expect(screen.getByPlaceholderText('비밀번호')).toBeDisabled()
      expect(screen.getByRole('button', { name: '회원가입' })).toBeDisabled()
    })
  })

  describe('이메일 인증', () => {
    it('이메일 미입력 시 발송하면 에러 메시지를 표시한다', async () => {
      const user = userEvent.setup()
      renderWithProviders(<JoinPage />)

      await user.click(screen.getByRole('button', { name: '인증코드 발송' }))

      await waitFor(() => {
        expect(screen.getByText('이메일을 입력해주세요')).toBeInTheDocument()
      })
    })

    it('이미 가입된 이메일로 발송하면 에러 메시지를 표시한다', async () => {
      const user = userEvent.setup()
      renderWithProviders(<JoinPage />)

      await user.type(screen.getByPlaceholderText('이메일'), 'duplicate@yologram.link')
      await user.click(screen.getByRole('button', { name: '인증코드 발송' }))

      await waitFor(() => {
        expect(screen.getByText('이미 가입된 이메일입니다.')).toBeInTheDocument()
      })
    })

    it('발송 성공 시 인증 코드 입력란이 노출된다', async () => {
      const user = userEvent.setup()
      renderWithProviders(<JoinPage />)

      await user.type(screen.getByPlaceholderText('이메일'), 'new@yologram.link')
      await user.click(screen.getByRole('button', { name: '인증코드 발송' }))

      expect(await screen.findByPlaceholderText('인증 코드 6자리')).toBeInTheDocument()
    })

    it('잘못된 코드로 인증하면 에러 메시지를 표시한다', async () => {
      const user = userEvent.setup()
      renderWithProviders(<JoinPage />)

      await user.type(screen.getByPlaceholderText('이메일'), 'new@yologram.link')
      await user.click(screen.getByRole('button', { name: '인증코드 발송' }))

      const codeInput = await screen.findByPlaceholderText('인증 코드 6자리')
      await user.type(codeInput, '000000')
      await user.click(screen.getByRole('button', { name: '인증 확인' }))

      await waitFor(() => {
        expect(screen.getByText('인증 코드가 일치하지 않습니다.')).toBeInTheDocument()
      })
    })

    it('인증 성공 시 가입 폼이 활성화된다', async () => {
      const user = userEvent.setup()
      renderWithProviders(<JoinPage />)

      await verifyEmailStep(user)

      expect(screen.getByPlaceholderText('이름')).toBeEnabled()
      expect(screen.getByPlaceholderText('닉네임')).toBeEnabled()
      expect(screen.getByPlaceholderText('비밀번호')).toBeEnabled()
      expect(screen.getByRole('button', { name: '회원가입' })).toBeEnabled()
    })
  })

  describe('회원가입', () => {
    it('인증 후 가입에 성공하면 로그인 페이지로 이동한다', async () => {
      const user = userEvent.setup()
      renderWithProviders(<JoinPage />)

      await verifyEmailStep(user)

      await user.type(screen.getByPlaceholderText('이름'), '테스트')
      await user.type(screen.getByPlaceholderText('닉네임'), 'tester')
      await user.type(screen.getByPlaceholderText('비밀번호'), 'password123!')
      await user.click(screen.getByRole('button', { name: '회원가입' }))

      await waitFor(() => {
        expect(mockNavigate).toHaveBeenCalledWith('/login')
      })
    })
  })
})
