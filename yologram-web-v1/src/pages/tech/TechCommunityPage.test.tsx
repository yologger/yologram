import { describe, it, expect, vi, beforeAll, afterEach, afterAll } from 'vitest'
import { screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { renderWithProviders } from '../../test/utils'
import { server } from '../../test/server'
import TechCommunityPage from './TechCommunityPage'

const mockNavigate = vi.fn()
vi.mock('react-router', async () => {
  const actual = await vi.importActual('react-router')
  return { ...actual, useNavigate: () => mockNavigate }
})

beforeAll(() => {
  server.listen()
  vi.stubGlobal(
    'IntersectionObserver',
    class {
      observe() {}
      unobserve() {}
      disconnect() {}
    },
  )
})
afterEach(() => server.resetHandlers())
afterAll(() => server.close())

describe('TechCommunityPage', () => {
  it('목록 API에서 받은 게시글과 작성바가 렌더링된다', async () => {
    renderWithProviders(<TechCommunityPage />)

    expect(await screen.findByText('API 피드 본문 1')).toBeInTheDocument()
    expect(screen.getByText('API 피드 본문 2')).toBeInTheDocument()
    expect(screen.getByRole('button', { name: '기술 커뮤니티에 글을 남겨보세요' })).toBeInTheDocument()
  })

  it('API에서 받은 카테고리가 필터 칩으로 표시된다', async () => {
    renderWithProviders(<TechCommunityPage />)

    // Frontend는 필터 칩과 게시글 배지에 모두 나타남 (id→name 매핑) → findAllByText로 대기
    expect((await screen.findAllByText('Frontend')).length).toBeGreaterThan(0)
    expect(screen.getByText('전체')).toBeInTheDocument()
    expect(screen.getAllByText('Backend').length).toBeGreaterThan(0)
  })

  it('카테고리 필터 선택 시 해당 카테고리 글만 조회한다', async () => {
    const user = userEvent.setup()
    renderWithProviders(<TechCommunityPage />)

    await screen.findByText('API 피드 본문 1')

    // 'Backend'(id=2) 필터 칩 클릭 → categoryId=2 글(본문 2)만 남음
    const backendChip = screen.getAllByText('Backend')[0]
    await user.click(backendChip)

    expect(await screen.findByText('API 피드 본문 2')).toBeInTheDocument()
    expect(screen.queryByText('API 피드 본문 1')).not.toBeInTheDocument()
  })

  it('작성바 클릭 시 글 작성 페이지로 이동한다', async () => {
    const user = userEvent.setup()
    renderWithProviders(<TechCommunityPage />)

    await user.click(screen.getByRole('button', { name: '기술 커뮤니티에 글을 남겨보세요' }))

    expect(mockNavigate).toHaveBeenCalledWith('/tech/community/write')
  })
})
