import { describe, it, expect, vi, beforeAll, afterEach, afterAll } from 'vitest'
import { screen, waitFor, within } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { http, HttpResponse } from 'msw'
import { getDefaultStore } from 'jotai'
import { renderWithProviders } from '../../test/utils'
import { server } from '../../test/server'
import { authAtom } from '../../stores/auth'
import TechCommunityPage from './TechCommunityPage'

const mockNavigate = vi.fn()
vi.mock('react-router', async () => {
  const actual = await vi.importActual('react-router')
  return { ...actual, useNavigate: () => mockNavigate }
})

const login = () =>
  getDefaultStore().set(authAtom, {
    uid: 1,
    accessToken: 'valid-token',
    email: 'test@yologram.link',
    name: '테스터',
    nickname: 'tester',
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
afterEach(() => {
  server.resetHandlers()
  mockNavigate.mockClear()
  getDefaultStore().set(authAtom, null)
})
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

  // 목록 msw 기본값: 첫 글(id 1050) metrics { likeCount: 3, likedByMe: false }
  it('카드 하트 클릭 시 즉시 카운트가 증가하고(옵티미스틱) 상세로 이동하지 않는다', async () => {
    login()
    let likeCalled = false
    server.use(
      http.post('http://localhost:5001/api/v1/pms/:section/posts/:id/like', () => {
        likeCalled = true
        return new HttpResponse(null, { status: 200 })
      }),
    )
    const user = userEvent.setup()
    renderWithProviders(<TechCommunityPage />)

    await screen.findByText('API 피드 본문 1')
    const likeButton = screen.getAllByRole('button', { name: '좋아요' })[0]
    expect(likeButton).toHaveTextContent('3')

    await user.click(likeButton)

    // 옵티미스틱: 목록 캐시가 즉시 갱신되어 카운트 증가
    await waitFor(() => expect(likeButton).toHaveTextContent('4'))
    expect(likeButton).toHaveAttribute('aria-pressed', 'true')
    await waitFor(() => expect(likeCalled).toBe(true))
    // stopPropagation → 카드 클릭(상세 이동) 미발생
    expect(mockNavigate).not.toHaveBeenCalled()
  })

  it('카드 하트 API 실패 시 카운트를 원복하고 에러 메시지를 표시한다', async () => {
    login()
    server.use(
      http.post('http://localhost:5001/api/v1/pms/:section/posts/:id/like', () =>
        HttpResponse.json(
          { errorMessage: '서버 오류', errorCode: 'INTERNAL_SERVER_ERROR' },
          { status: 500 },
        ),
      ),
    )
    const user = userEvent.setup()
    renderWithProviders(<TechCommunityPage />)

    await screen.findByText('API 피드 본문 1')
    const likeButton = screen.getAllByRole('button', { name: '좋아요' })[0]

    await user.click(likeButton)

    // 실패 → 스냅샷 원복 + 토스트
    expect(await screen.findByText(/좋아요 처리에 실패했어요/)).toBeInTheDocument()
    await waitFor(() => expect(likeButton).toHaveTextContent('3'))
    expect(likeButton).toHaveAttribute('aria-pressed', 'false')
  })

  it('비로그인 상태에서 카드 하트 클릭 시 로그인 유도 모달을 띄우고 API를 호출하지 않는다', async () => {
    let likeCalled = false
    server.use(
      http.post('http://localhost:5001/api/v1/pms/:section/posts/:id/like', () => {
        likeCalled = true
        return new HttpResponse(null, { status: 200 })
      }),
    )
    const user = userEvent.setup()
    renderWithProviders(<TechCommunityPage />)

    await screen.findByText('API 피드 본문 1')

    // 비로그인에도 하트 버튼은 활성 (disabled 아님)
    const likeButton = screen.getAllByRole('button', { name: '좋아요' })[0]
    expect(likeButton).toBeEnabled()

    await user.click(likeButton)

    // 로그인 유도 모달 노출 + 카운트 그대로, API 미호출, 상세 이동도 없음
    const dialogs = await screen.findAllByRole('dialog')
    const dialog = dialogs[dialogs.length - 1]
    // antd confirm은 제목을 header/본문에 두 번 렌더링 → getAllByText 사용
    expect(within(dialog).getAllByText('로그인이 필요해요').length).toBeGreaterThan(0)
    expect(likeButton).toHaveTextContent('3')
    expect(likeCalled).toBe(false)
    expect(mockNavigate).not.toHaveBeenCalled()

    // 확인 시 returnTo와 함께 로그인 페이지로 이동
    await user.click(within(dialog).getByRole('button', { name: '로그인' }))
    await waitFor(() =>
      expect(mockNavigate).toHaveBeenCalledWith('/login', { state: { returnTo: '/' } }),
    )
  })
})
