import { describe, it, expect, beforeAll, afterAll, afterEach, beforeEach, vi } from 'vitest'
import { screen, waitFor } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { getDefaultStore } from 'jotai'
import { server } from '../../test/server'
import { renderWithProviders } from '../../test/utils'
import { authAtom } from '../../stores/auth'
import EditProfilePage from './EditProfilePage'

const mockNavigate = vi.fn()
vi.mock('react-router', async () => {
  const actual = await vi.importActual('react-router')
  return { ...actual, useNavigate: () => mockNavigate }
})

beforeAll(() => server.listen())
beforeEach(() => {
  getDefaultStore().set(authAtom, {
    uid: 1,
    accessToken: 'valid-token',
    email: 'test@yologram.link',
    name: '테스터',
    nickname: 'tester',
  })
})
afterEach(() => {
  server.resetHandlers()
  mockNavigate.mockClear()
  getDefaultStore().set(authAtom, null)
})
afterAll(() => server.close())

describe('EditProfilePage', () => {
  describe('렌더링', () => {
    it('현재 회원정보를 불러와 표시한다', async () => {
      renderWithProviders(<EditProfilePage />)

      await waitFor(() => {
        expect(screen.getByDisplayValue('tester')).toBeInTheDocument()
      })
      expect(screen.getByDisplayValue('test@yologram.link')).toBeDisabled()
      expect(screen.getByDisplayValue('테스터')).toBeDisabled()
    })
  })

  describe('입력값 검증', () => {
    it('닉네임이 2자 미만이면 저장 버튼이 비활성화된다', async () => {
      const user = userEvent.setup()
      renderWithProviders(<EditProfilePage />)

      const input = await screen.findByDisplayValue('tester')
      await user.clear(input)
      await user.type(input, 'a')

      expect(screen.getByRole('button', { name: '저장' })).toBeDisabled()
    })

    it('닉네임이 21자 이상이면 저장 버튼이 비활성화된다', async () => {
      const user = userEvent.setup()
      renderWithProviders(<EditProfilePage />)

      const input = await screen.findByDisplayValue('tester')
      await user.clear(input)
      await user.type(input, 'a'.repeat(21))

      expect(screen.getByRole('button', { name: '저장' })).toBeDisabled()
    })
  })

  describe('수정', () => {
    it('성공 시 설정 페이지로 이동한다', async () => {
      const user = userEvent.setup()
      renderWithProviders(<EditProfilePage />)

      const input = await screen.findByDisplayValue('tester')
      await user.clear(input)
      await user.type(input, 'new-nickname')
      await user.click(screen.getByRole('button', { name: '저장' }))

      await waitFor(() => {
        expect(mockNavigate).toHaveBeenCalledWith('/settings')
      })
    })
  })
})
