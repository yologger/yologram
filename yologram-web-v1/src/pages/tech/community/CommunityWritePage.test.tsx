import { describe, it, expect, vi, beforeAll, afterEach, afterAll } from 'vitest'
import { screen, waitFor } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { renderWithProviders } from '../../../test/utils'
import { server } from '../../../test/server'
import CommunityWritePage from './CommunityWritePage'

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

describe('CommunityWritePage', () => {
  it('내용이 비어있으면 남기기 버튼이 비활성화된다', () => {
    renderWithProviders(<CommunityWritePage />)
    expect(screen.getByRole('button', { name: '남기기' })).toBeDisabled()
  })

  it('내용 입력 후 카테고리 선택해 남기기 시 피드로 이동한다', async () => {
    const user = userEvent.setup()
    renderWithProviders(<CommunityWritePage />)

    await user.type(screen.getByPlaceholderText(/광고, 비난/), '테스트 글 내용')
    // 카테고리 로드 후 선택
    await user.click(await screen.findByText('Frontend'))
    await user.click(screen.getByRole('button', { name: '남기기' }))

    await waitFor(() => {
      expect(mockNavigate).toHaveBeenCalledWith('/tech/community')
    })
  })

  it('내용만 입력하고 카테고리 미선택 시 남기기 버튼이 비활성화된다', async () => {
    const user = userEvent.setup()
    renderWithProviders(<CommunityWritePage />)

    await user.type(screen.getByPlaceholderText(/광고, 비난/), '카테고리 미선택 글')
    // 카테고리 로드 후에도 미선택이면 비활성
    await screen.findByText('Frontend')

    expect(screen.getByRole('button', { name: '남기기' })).toBeDisabled()
  })
})
