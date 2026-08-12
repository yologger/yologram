import { describe, it, expect, beforeAll, afterAll, afterEach, beforeEach, vi } from 'vitest'
import { screen, waitFor, within } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { http, HttpResponse } from 'msw'
import { QueryClient } from '@tanstack/react-query'
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

// 모달 portal이 테스트 간 body에 누적될 수 있어, 가장 최근에 열린 dialog를 사용
const latestDialog = async () => {
  const dialogs = await screen.findAllByRole('dialog')
  return dialogs[dialogs.length - 1]
}

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

  it('삭제 버튼 클릭 후 확인 모달에서 삭제하면 목록에서 제거된다', async () => {
    const user = userEvent.setup()
    renderWithProviders(<MyPostsPage />)

    await screen.findByText('내가 쓴 기술 글')

    // 삭제 성공 후 invalidate 시 재조회되는 목록에서 해당 글 제거
    server.use(
      http.get('http://localhost:5001/api/v1/pms/posts/me', () =>
        HttpResponse.json({
          data: [
            { id: 2002, section: 'INVEST', author: { uid: 1, nickname: '테스터' }, content: '내가 쓴 투자 글', categoryIds: [9], metrics: { commentCount: 2, likeCount: 5, likedByMe: false }, createdAt: '2026-06-17T09:00:00' },
            { id: 2003, section: 'POLITICS', author: { uid: 1, nickname: '테스터' }, content: '내가 쓴 정치 글', categoryIds: [16], metrics: { commentCount: 0, likeCount: 1, likedByMe: false }, createdAt: '2026-06-16T09:00:00' },
          ],
          nextCursor: null,
        }),
      ),
    )

    // 첫 번째 글(기술 글)의 삭제 버튼 클릭
    const deleteButtons = screen.getAllByRole('button', { name: '삭제' })
    await user.click(deleteButtons[0])

    const dialog = await latestDialog()
    await user.click(within(dialog).getByRole('button', { name: '삭제' }))

    await waitFor(() => {
      expect(screen.queryByText('내가 쓴 기술 글')).not.toBeInTheDocument()
    })
    expect(screen.getByText('내가 쓴 투자 글')).toBeInTheDocument()
  })

  it('게시글 삭제 성공 시 해당 글의 댓글 캐시를 제거한다', async () => {
    // 삭제된 글은 다시 볼 일이 없으므로 removeQueries 호출을 검증
    const removeSpy = vi.spyOn(QueryClient.prototype, 'removeQueries')
    const user = userEvent.setup()
    renderWithProviders(<MyPostsPage />)

    await screen.findByText('내가 쓴 기술 글')

    // 첫 번째 글(기술 글, id 2001)의 삭제 버튼 클릭
    const deleteButtons = screen.getAllByRole('button', { name: '삭제' })
    await user.click(deleteButtons[0])

    const dialog = await latestDialog()
    await user.click(within(dialog).getByRole('button', { name: '삭제' }))

    await waitFor(() => {
      expect(removeSpy).toHaveBeenCalledWith({ queryKey: ['comments', 'tech', 2001] })
    })
    removeSpy.mockRestore()
  })

  it('삭제 확인 모달에서 취소하면 글이 그대로 남는다', async () => {
    const user = userEvent.setup()
    renderWithProviders(<MyPostsPage />)

    await screen.findByText('내가 쓴 기술 글')

    const deleteButtons = screen.getAllByRole('button', { name: '삭제' })
    await user.click(deleteButtons[0])

    const dialog = await latestDialog()
    await user.click(within(dialog).getByRole('button', { name: '취소' }))

    // 취소했으므로 목록이 그대로 유지된다
    expect(screen.getByText('내가 쓴 기술 글')).toBeInTheDocument()
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
