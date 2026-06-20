import { describe, it, expect, vi, beforeAll, afterEach, afterAll, beforeEach } from 'vitest'
import { screen, waitFor } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { renderWithProviders } from '../../../../../test/utils'
import { server } from '../../../../../test/server'
import CommunityDetail from './page'

const mockUseParams = vi.fn()
vi.mock('next/navigation', () => ({
  useRouter: () => ({ back: vi.fn(), push: vi.fn() }),
  useParams: () => mockUseParams(),
}))

beforeAll(() => server.listen())
afterEach(() => server.resetHandlers())
afterAll(() => server.close())
beforeEach(() => mockUseParams.mockReturnValue({ postId: '1' }))

describe('CommunityDetail', () => {
  it('API로 조회한 게시글 본문과 댓글 입력을 표시한다', async () => {
    renderWithProviders(<CommunityDetail />)

    expect(await screen.findByText('API 본문 내용')).toBeInTheDocument()
    expect(screen.getByText('테스터')).toBeInTheDocument()
    expect(screen.getByPlaceholderText('댓글로 의견을 남겨보세요')).toBeInTheDocument()
  })

  it('댓글을 입력하고 등록하면 목록에 추가된다', async () => {
    const user = userEvent.setup()
    renderWithProviders(<CommunityDetail />)

    await screen.findByText('API 본문 내용')

    await user.type(screen.getByPlaceholderText('댓글로 의견을 남겨보세요'), '좋은 글이네요')
    await user.click(screen.getByRole('button', { name: '등록' }))

    await waitFor(() => {
      expect(screen.getByText('좋은 글이네요')).toBeInTheDocument()
    })
  })

  it('존재하지 않는 글이면 안내 문구를 표시한다', async () => {
    mockUseParams.mockReturnValue({ postId: '99999' })
    renderWithProviders(<CommunityDetail />)

    expect(await screen.findByText('존재하지 않는 글입니다.')).toBeInTheDocument()
  })
})
