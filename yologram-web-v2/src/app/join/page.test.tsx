import { describe, it, expect, beforeAll, afterAll, afterEach, vi } from 'vitest'
import { screen, waitFor } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { server } from '../../test/server'
import { renderWithProviders } from '../../test/utils'
import JoinPage from './page'

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

describe('JoinPage', () => {
  describe('렌더링', () => {
    it('회원가입 폼이 렌더링된다', () => {
      renderWithProviders(<JoinPage />)

      expect(screen.getByPlaceholderText('이메일')).toBeInTheDocument()
      expect(screen.getByPlaceholderText('이름')).toBeInTheDocument()
      expect(screen.getByPlaceholderText('닉네임')).toBeInTheDocument()
      expect(screen.getByPlaceholderText('비밀번호')).toBeInTheDocument()
      expect(screen.getByRole('button', { name: '회원가입' })).toBeInTheDocument()
      expect(screen.getByText('로그인')).toBeInTheDocument()
    })
  })

  describe('입력값 검증', () => {
    it('필수 필드가 비어있으면 에러 메시지를 표시한다', async () => {
      const user = userEvent.setup()
      renderWithProviders(<JoinPage />)

      await user.click(screen.getByRole('button', { name: '회원가입' }))

      await waitFor(() => {
        expect(screen.getByText('이메일을 입력해주세요')).toBeInTheDocument()
      })
    })
  })

  describe('회원가입 성공', () => {
    it('성공 시 로그인 페이지로 이동한다', async () => {
      const user = userEvent.setup()
      renderWithProviders(<JoinPage />)

      await user.type(screen.getByPlaceholderText('이메일'), 'new@yologram.link')
      await user.type(screen.getByPlaceholderText('이름'), '테스트')
      await user.type(screen.getByPlaceholderText('닉네임'), 'tester')
      await user.type(screen.getByPlaceholderText('비밀번호'), 'password123!')
      await user.click(screen.getByRole('button', { name: '회원가입' }))

      await waitFor(() => {
        expect(mockPush).toHaveBeenCalledWith('/login')
      })
    })
  })

  describe('회원가입 실패', () => {
    it('이메일 중복 시 에러 메시지를 표시한다', async () => {
      const user = userEvent.setup()
      renderWithProviders(<JoinPage />)

      await user.type(screen.getByPlaceholderText('이메일'), 'duplicate@yologram.link')
      await user.type(screen.getByPlaceholderText('이름'), '테스트')
      await user.type(screen.getByPlaceholderText('닉네임'), 'tester')
      await user.type(screen.getByPlaceholderText('비밀번호'), 'password123!')
      await user.click(screen.getByRole('button', { name: '회원가입' }))

      await waitFor(() => {
        expect(screen.getByText('이미 가입된 이메일입니다.')).toBeInTheDocument()
      })
    })
  })
})
