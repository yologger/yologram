import { describe, it, expect, vi } from 'vitest'
import { screen, waitFor } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { renderWithProviders } from '../../../test/utils'
import CommunityDetailPage from './CommunityDetailPage'

vi.mock('react-router', async () => {
  const actual = await vi.importActual('react-router')
  return { ...actual, useNavigate: () => vi.fn(), useParams: () => ({ postId: '1000' }) }
})

describe('CommunityDetailPage', () => {
  it('게시글 본문과 댓글 영역을 표시한다', () => {
    renderWithProviders(<CommunityDetailPage />)

    expect(screen.getByText('qld보다 이게 더 좋나요 음의복리도 없고 수익률도 더 높던데')).toBeInTheDocument()
    expect(screen.getByPlaceholderText('댓글로 의견을 남겨보세요')).toBeInTheDocument()
  })

  it('댓글을 입력하고 등록하면 목록에 추가된다', async () => {
    const user = userEvent.setup()
    renderWithProviders(<CommunityDetailPage />)

    await user.type(screen.getByPlaceholderText('댓글로 의견을 남겨보세요'), '좋은 글이네요')
    await user.click(screen.getByRole('button', { name: '등록' }))

    await waitFor(() => {
      expect(screen.getByText('좋은 글이네요')).toBeInTheDocument()
    })
  })
})
