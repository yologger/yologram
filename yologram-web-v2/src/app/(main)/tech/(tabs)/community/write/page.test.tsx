import { describe, it, expect, vi, beforeAll, beforeEach, afterEach, afterAll } from 'vitest'
import { screen, waitFor } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { getDefaultStore } from 'jotai'
import { renderWithProviders } from '../../../../../test/utils'
import { server } from '../../../../../test/server'
import { authAtom } from '@/stores/auth'
import CommunityWrite from './page'

const mockPush = vi.fn()
const mockReplace = vi.fn()
vi.mock('next/navigation', () => ({
  useRouter: () => ({ push: mockPush, replace: mockReplace }),
}))

const store = getDefaultStore()

beforeAll(() => server.listen())
beforeEach(() => {
  store.set(authAtom, { uid: 1, email: 't@yologram.link', name: '테스터', nickname: 'tester', accessToken: 't' })
})
afterEach(() => {
  server.resetHandlers()
  store.set(authAtom, null)
  mockPush.mockClear()
  mockReplace.mockClear()
})
afterAll(() => server.close())

describe('CommunityWrite', () => {
  it('내용이 비어있으면 남기기 버튼이 비활성화된다', async () => {
    renderWithProviders(<CommunityWrite />)
    expect(await screen.findByRole('button', { name: '남기기' })).toBeDisabled()
  })

  it('내용 입력 후 카테고리 선택해 남기기 시 피드로 이동한다', async () => {
    const user = userEvent.setup()
    renderWithProviders(<CommunityWrite />)

    await user.type(await screen.findByPlaceholderText(/광고, 비난/), '테스트 글 내용')
    await user.click(await screen.findByText('Frontend'))
    await user.click(screen.getByRole('button', { name: '남기기' }))

    await waitFor(() => {
      expect(mockPush).toHaveBeenCalledWith('/tech/community')
    })
  })

  it('내용만 입력하고 카테고리 미선택 시 남기기 버튼이 비활성화된다', async () => {
    const user = userEvent.setup()
    renderWithProviders(<CommunityWrite />)

    await user.type(await screen.findByPlaceholderText(/광고, 비난/), '카테고리 미선택 글')
    await screen.findByText('Frontend')

    expect(screen.getByRole('button', { name: '남기기' })).toBeDisabled()
  })
})
