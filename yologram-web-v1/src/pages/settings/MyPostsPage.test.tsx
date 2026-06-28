import { describe, it, expect, beforeAll, afterAll, afterEach, beforeEach, vi } from 'vitest'
import { screen, waitFor } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { http, HttpResponse } from 'msw'
import { getDefaultStore } from 'jotai'
import { server } from '../../test/server'
import { renderWithProviders } from '../../test/utils'
import { authAtom } from '../../stores/auth'
import MyPostsPage from './MyPostsPage'

const mockNavigate = vi.fn()
vi.mock('react-router', async () => {
  const actual = await vi.importActual('react-router')
  return { ...actual, useNavigate: () => mockNavigate }
})

// jsdom에는 IntersectionObserver가 없어 스텁 처리 (무한스크롤 센티넬용)
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
afterAll(() => {
  server.close()
  vi.unstubAllGlobals()
})

describe('MyPostsPage', () => {
  it('전체 탭에서 내 글을 모든 섹션에 걸쳐 표시한다', async () => {
    renderWithProviders(<MyPostsPage />)

    expect(await screen.findByText('내가 쓴 기술 글')).toBeInTheDocument()
    expect(screen.getByText('내가 쓴 투자 글')).toBeInTheDocument()
    expect(screen.getByText('내가 쓴 정치 글')).toBeInTheDocument()
  })

  it('카테고리 id를 이름으로 변환해 뱃지로 표시한다', async () => {
    renderWithProviders(<MyPostsPage />)

    // tech categoryIds[1] → 'Frontend', invest [9] → '해외주식'
    expect(await screen.findByText('Frontend')).toBeInTheDocument()
    expect(screen.getByText('해외주식')).toBeInTheDocument()
  })

  it('섹션 탭을 선택하면 해당 섹션 글만 표시한다', async () => {
    const user = userEvent.setup()
    renderWithProviders(<MyPostsPage />)

    await screen.findByText('내가 쓴 기술 글')

    await user.click(screen.getByText('투자'))

    await waitFor(() => {
      expect(screen.getByText('내가 쓴 투자 글')).toBeInTheDocument()
    })
    expect(screen.queryByText('내가 쓴 기술 글')).not.toBeInTheDocument()
    expect(screen.queryByText('내가 쓴 정치 글')).not.toBeInTheDocument()
  })

  it('TECH 글 클릭 시 상세로 이동하고, 비TECH 글은 이동하지 않는다', async () => {
    const user = userEvent.setup()
    renderWithProviders(<MyPostsPage />)

    await user.click(await screen.findByText('내가 쓴 기술 글'))
    expect(mockNavigate).toHaveBeenCalledWith('/tech/community/2001')

    mockNavigate.mockClear()
    await user.click(screen.getByText('내가 쓴 투자 글'))
    expect(mockNavigate).not.toHaveBeenCalled()
  })

  it('작성한 글이 없으면 빈 상태 문구를 표시한다', async () => {
    server.use(
      http.get('http://localhost:5001/api/v1/pms/posts/me', () =>
        HttpResponse.json({ data: [], nextCursor: null }),
      ),
    )
    renderWithProviders(<MyPostsPage />)

    expect(await screen.findByText('작성한 글이 없어요')).toBeInTheDocument()
  })

  it('조회 실패 시 에러 문구와 다시 시도 버튼을 표시한다', async () => {
    server.use(
      http.get('http://localhost:5001/api/v1/pms/posts/me', () =>
        HttpResponse.json(
          { errorMessage: '서버 오류', errorCode: 'INTERNAL_ERROR' },
          { status: 500 },
        ),
      ),
    )
    renderWithProviders(<MyPostsPage />)

    expect(await screen.findByText(/글을 불러오지 못했어요/)).toBeInTheDocument()
    expect(screen.getByRole('button', { name: '다시 시도' })).toBeInTheDocument()
  })
})
