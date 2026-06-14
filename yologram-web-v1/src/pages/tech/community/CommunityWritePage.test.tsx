import { describe, it, expect, vi } from 'vitest'
import { screen, waitFor } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { renderWithProviders } from '../../../test/utils'
import CommunityWritePage from './CommunityWritePage'

const mockNavigate = vi.fn()
vi.mock('react-router', async () => {
  const actual = await vi.importActual('react-router')
  return { ...actual, useNavigate: () => mockNavigate }
})

describe('CommunityWritePage', () => {
  it('내용이 비어있으면 남기기 버튼이 비활성화된다', () => {
    renderWithProviders(<CommunityWritePage />)
    expect(screen.getByRole('button', { name: '남기기' })).toBeDisabled()
  })

  it('내용 입력 후 남기기 시 피드로 이동한다', async () => {
    const user = userEvent.setup()
    renderWithProviders(<CommunityWritePage />)

    await user.type(screen.getByPlaceholderText(/광고, 비난/), '테스트 글 내용')
    await user.click(screen.getByRole('button', { name: '남기기' }))

    await waitFor(() => {
      expect(mockNavigate).toHaveBeenCalledWith('/tech/community')
    })
  })
})
