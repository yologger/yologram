import { describe, it, expect, vi, beforeAll, afterEach, afterAll } from 'vitest'
import { screen, waitFor } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { http, HttpResponse } from 'msw'
import { getDefaultStore } from 'jotai'
import { renderWithProviders } from '../../../../../test/utils'
import { server } from '../../../../../test/server'
import { authAtom } from '@/stores/auth'
import TechCommunity from './page'

const mockPush = vi.fn()
vi.mock('next/navigation', () => ({
  useRouter: () => ({ push: mockPush }),
}))

const store = getDefaultStore()

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
afterEach(() => {
  server.resetHandlers()
  store.set(authAtom, null)
  mockPush.mockClear()
})
afterAll(() => server.close())

describe('TechCommunity 피드', () => {
  it('목록 API에서 받은 게시글과 작성바가 렌더링된다', async () => {
    renderWithProviders(<TechCommunity />)

    expect(await screen.findByText('API 피드 본문 1')).toBeInTheDocument()
    expect(screen.getByText('API 피드 본문 2')).toBeInTheDocument()
    expect(screen.getByRole('button', { name: '기술 커뮤니티에 글을 남겨보세요' })).toBeInTheDocument()
  })

  it('API에서 받은 카테고리가 필터 칩으로 표시된다', async () => {
    renderWithProviders(<TechCommunity />)

    // Frontend는 필터 칩과 게시글 배지에 모두 나타남 (id→name 매핑) → findAllByText로 대기
    expect((await screen.findAllByText('Frontend')).length).toBeGreaterThan(0)
    expect(screen.getByText('전체')).toBeInTheDocument()
    expect(screen.getAllByText('Backend').length).toBeGreaterThan(0)
  })

  it('카테고리 필터 선택 시 해당 카테고리 글만 조회한다', async () => {
    const user = userEvent.setup()
    renderWithProviders(<TechCommunity />)

    await screen.findByText('API 피드 본문 1')

    // 'Backend'(id=2) 필터 칩 클릭 → categoryId=2 글(본문 2)만 남음
    const backendChip = screen.getAllByText('Backend')[0]
    await user.click(backendChip)

    expect(await screen.findByText('API 피드 본문 2')).toBeInTheDocument()
    expect(screen.queryByText('API 피드 본문 1')).not.toBeInTheDocument()
  })

  it('작성바 클릭 시 글 작성 페이지로 이동한다', async () => {
    const user = userEvent.setup()
    renderWithProviders(<TechCommunity />)

    await user.click(screen.getByRole('button', { name: '기술 커뮤니티에 글을 남겨보세요' }))

    expect(mockPush).toHaveBeenCalledWith('/tech/community/write')
  })

  it('카드의 좋아요 수가 metrics 기반으로 표시된다', async () => {
    renderWithProviders(<TechCommunity />)

    await screen.findByText('API 피드 본문 1')
    const likeButtons = screen.getAllByRole('button', { name: '좋아요' })
    expect(likeButtons[0]).toHaveTextContent('3')
    expect(likeButtons[1]).toHaveTextContent('0')
  })

  it('로그인 상태에서 카드 하트 클릭 시 즉시 카운트가 증가하고 등록 요청이 발생하며 상세로 이동하지 않는다', async () => {
    let likedPath: { section: string; id: string } | null = null
    server.use(
      http.post('http://localhost:5002/api/v2/pms/:section/posts/:id/like', ({ params }) => {
        likedPath = { section: String(params.section), id: String(params.id) }
        return new HttpResponse(null, { status: 200 })
      }),
    )
    store.set(authAtom, { uid: 1, email: 't@yologram.link', name: '테스터', nickname: 'tester', accessToken: 't' })
    const user = userEvent.setup()
    renderWithProviders(<TechCommunity />)

    await screen.findByText('API 피드 본문 1')
    await user.click(screen.getAllByRole('button', { name: '좋아요' })[0])

    // 옵티미스틱: 목록 캐시에 즉시 반영 (3 → 4, 채워진 하트)
    await waitFor(() => expect(screen.getAllByRole('button', { name: '좋아요' })[0]).toHaveTextContent('4'))
    expect(screen.getAllByRole('button', { name: '좋아요' })[0]).toHaveAttribute('aria-pressed', 'true')
    await waitFor(() => expect(likedPath).toEqual({ section: 'tech', id: '1050' }))
    // 하트 클릭은 카드 클릭(상세 이동)으로 전파되지 않음
    expect(mockPush).not.toHaveBeenCalled()
  })

  it('카드 하트 클릭 실패 시 카운트를 원복하고 에러 토스트를 표시한다', async () => {
    server.use(
      http.post('http://localhost:5002/api/v2/pms/:section/posts/:id/like', () =>
        HttpResponse.json(
          { errorMessage: '서버 오류가 발생했습니다.', errorCode: 'INTERNAL_SERVER_ERROR' },
          { status: 500 },
        ),
      ),
    )
    store.set(authAtom, { uid: 1, email: 't@yologram.link', name: '테스터', nickname: 'tester', accessToken: 't' })
    const user = userEvent.setup()
    renderWithProviders(<TechCommunity />)

    await screen.findByText('API 피드 본문 1')
    await user.click(screen.getAllByRole('button', { name: '좋아요' })[0])

    expect(await screen.findByText(/좋아요 처리에 실패했어요/)).toBeInTheDocument()
    // 스냅샷 원복으로 원래 카운트 유지
    await waitFor(() => expect(screen.getAllByRole('button', { name: '좋아요' })[0]).toHaveTextContent('3'))
    expect(screen.getAllByRole('button', { name: '좋아요' })[0]).toHaveAttribute('aria-pressed', 'false')
  })

  it('비로그인 상태에서는 카드 하트 버튼이 비활성화되고 클릭해도 요청하지 않는다', async () => {
    let called = false
    server.use(
      http.post('http://localhost:5002/api/v2/pms/:section/posts/:id/like', () => {
        called = true
        return new HttpResponse(null, { status: 200 })
      }),
    )
    // 비로그인 상태 유지(afterEach에서 null 설정)
    const user = userEvent.setup()
    renderWithProviders(<TechCommunity />)

    await screen.findByText('API 피드 본문 1')
    const likeButton = screen.getAllByRole('button', { name: '좋아요' })[0]
    expect(likeButton).toBeDisabled()

    await user.click(likeButton).catch(() => {})

    await new Promise((r) => setTimeout(r, 50))
    expect(called).toBe(false)
    expect(likeButton).toHaveTextContent('3')
  })

  it('로그인 상태에서는 카드 하트 버튼이 활성화된다', async () => {
    store.set(authAtom, { uid: 1, email: 't@yologram.link', name: '테스터', nickname: 'tester', accessToken: 't' })
    renderWithProviders(<TechCommunity />)

    await screen.findByText('API 피드 본문 1')
    expect(screen.getAllByRole('button', { name: '좋아요' })[0]).toBeEnabled()
  })
})
