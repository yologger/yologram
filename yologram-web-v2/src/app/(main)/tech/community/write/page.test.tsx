import { describe, it, expect, vi } from 'vitest'
import { screen, waitFor } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { renderWithProviders } from '../../../../../test/utils'
import CommunityWrite from './page'

const mockPush = vi.fn()
vi.mock('next/navigation', () => ({
  useRouter: () => ({ push: mockPush }),
}))

describe('CommunityWrite', () => {
  it('내용이 비어있으면 남기기 버튼이 비활성화된다', () => {
    renderWithProviders(<CommunityWrite />)
    expect(screen.getByRole('button', { name: '남기기' })).toBeDisabled()
  })

  it('내용 입력 후 남기기 시 피드로 이동한다', async () => {
    const user = userEvent.setup()
    renderWithProviders(<CommunityWrite />)

    await user.type(screen.getByPlaceholderText(/광고, 비난/), '테스트 글 내용')
    await user.click(screen.getByRole('button', { name: '남기기' }))

    await waitFor(() => {
      expect(mockPush).toHaveBeenCalledWith('/tech/community')
    })
  })
})
