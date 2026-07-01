import { describe, it, expect, vi, beforeAll, afterEach, afterAll, beforeEach } from 'vitest'
import { screen, waitFor, within } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { http, HttpResponse } from 'msw'
import { getDefaultStore } from 'jotai'
import { renderWithProviders } from '../../../test/utils'
import { server } from '../../../test/server'
import { authAtom } from '../../../stores/auth'
import CommunityDetailPage from './CommunityDetailPage'

const mockNavigate = vi.fn()
const mockUseParams = vi.fn()
vi.mock('react-router', async () => {
  const actual = await vi.importActual('react-router')
  return { ...actual, useNavigate: () => mockNavigate, useParams: () => mockUseParams() }
})

const loginAs = (uid: number) =>
  getDefaultStore().set(authAtom, {
    uid,
    accessToken: 'valid-token',
    email: 'test@yologram.link',
    name: '테스터',
    nickname: 'tester',
  })

// jsdom에는 IntersectionObserver가 없어 스텁 처리 (무한스크롤 센티넬용).
// 관찰 콜백을 저장해 테스트에서 교차(intersect)를 수동으로 트리거한다.
let intersect: (() => void) | null = null
beforeAll(() => {
  server.listen()
  vi.stubGlobal(
    'IntersectionObserver',
    class {
      constructor(cb: (entries: { isIntersecting: boolean }[]) => void) {
        intersect = () => cb([{ isIntersecting: true }])
      }
      observe() {}
      unobserve() {}
      disconnect() {}
    },
  )
})
afterEach(() => {
  server.resetHandlers()
  mockNavigate.mockClear()
  intersect = null
  getDefaultStore().set(authAtom, null)
})
afterAll(() => {
  server.close()
  vi.unstubAllGlobals()
})
beforeEach(() => mockUseParams.mockReturnValue({ postId: '1' }))

// 모달 portal이 테스트 간 body에 누적될 수 있어, 가장 최근에 열린 dialog를 사용
const latestDialog = async () => {
  const dialogs = await screen.findAllByRole('dialog')
  return dialogs[dialogs.length - 1]
}

describe('CommunityDetailPage', () => {
  it('API로 조회한 게시글 본문과 댓글 목록을 표시한다', async () => {
    loginAs(1)
    renderWithProviders(<CommunityDetailPage />)

    expect(await screen.findByText('API 본문 내용')).toBeInTheDocument()
    expect(screen.getByPlaceholderText('댓글로 의견을 남겨보세요')).toBeInTheDocument()
    // 조회한 댓글 목록 렌더 (최신순 기본)
    expect(await screen.findByText('최신 댓글')).toBeInTheDocument()
    expect(screen.getByText('오래된 댓글')).toBeInTheDocument()
    expect(screen.getByText('다른유저')).toBeInTheDocument()
  })

  it('정렬을 오래된순으로 바꾸면 재조회하여 순서가 바뀐다', async () => {
    loginAs(1)
    const user = userEvent.setup()
    renderWithProviders(<CommunityDetailPage />)

    await screen.findByText('최신 댓글')
    // 최신순 기본: 최신 댓글이 오래된 댓글보다 앞
    const before = screen.getAllByText(/댓글$/).map((el) => el.textContent)
    expect(before.indexOf('최신 댓글')).toBeLessThan(before.indexOf('오래된 댓글'))

    await user.click(screen.getByRole('button', { name: '오래된순' }))

    // 재조회 후 순서 반전
    await waitFor(() => {
      const after = screen.getAllByText(/댓글$/).map((el) => el.textContent)
      expect(after.indexOf('오래된 댓글')).toBeLessThan(after.indexOf('최신 댓글'))
    })
  })

  it('센티넬 교차 시 다음 페이지를 불러온다 (무한스크롤)', async () => {
    server.use(
      http.get('http://localhost:5001/api/v1/comments/posts/:postId', ({ request }) => {
        const cursor = new URL(request.url).searchParams.get('cursor')
        if (cursor) {
          // 두 번째 페이지 (마지막)
          return HttpResponse.json({
            data: [{ id: 300, postId: 1, author: { uid: 3, nickname: '세번째' }, content: '다음 페이지 댓글', createdAt: '2026-06-18T12:00:00' }],
            nextCursor: null,
          })
        }
        return HttpResponse.json({
          data: [{ id: 100, postId: 1, author: { uid: 1, nickname: '테스터' }, content: '첫 페이지 댓글', createdAt: '2026-06-20T12:00:00' }],
          nextCursor: 'next-cursor',
        })
      }),
    )
    renderWithProviders(<CommunityDetailPage />)

    expect(await screen.findByText('첫 페이지 댓글')).toBeInTheDocument()
    expect(screen.queryByText('다음 페이지 댓글')).not.toBeInTheDocument()

    // 센티넬 교차 → fetchNextPage
    intersect?.()

    expect(await screen.findByText('다음 페이지 댓글')).toBeInTheDocument()
  })

  it('댓글이 없으면 빈 목록 안내를 표시한다', async () => {
    server.use(
      http.get('http://localhost:5001/api/v1/comments/posts/:postId', () =>
        HttpResponse.json({ data: [], nextCursor: null }),
      ),
    )
    renderWithProviders(<CommunityDetailPage />)

    expect(await screen.findByText(/아직 댓글이 없어요/)).toBeInTheDocument()
  })

  it('댓글 조회 실패 시 다시 시도 안내를 표시한다', async () => {
    server.use(
      http.get('http://localhost:5001/api/v1/comments/posts/:postId', () =>
        HttpResponse.json(
          { errorMessage: '서버 오류', errorCode: 'INTERNAL_SERVER_ERROR' },
          { status: 500 },
        ),
      ),
    )
    renderWithProviders(<CommunityDetailPage />)

    expect(await screen.findByText(/댓글을 불러오지 못했어요/)).toBeInTheDocument()
  })

  it('로그인 상태에서 댓글을 등록하면 성공 피드백을 표시하고 입력창을 비운다', async () => {
    loginAs(1)
    const user = userEvent.setup()
    renderWithProviders(<CommunityDetailPage />)

    // 게시글 로드 대기
    await screen.findByText('API 본문 내용')

    const input = screen.getByPlaceholderText('댓글로 의견을 남겨보세요')
    await user.type(input, '좋은 글이네요')
    await user.click(screen.getByRole('button', { name: '등록' }))

    // 성공 토스트 + 입력창 초기화
    expect(await screen.findByText('댓글이 등록되었습니다.')).toBeInTheDocument()
    await waitFor(() => expect(input).toHaveValue(''))
  })

  it('댓글 등록 성공 시 목록을 무효화하여 다시 조회한다', async () => {
    loginAs(1)
    let getCount = 0
    server.use(
      http.get('http://localhost:5001/api/v1/comments/posts/:postId', ({ request }) => {
        const cursor = new URL(request.url).searchParams.get('cursor')
        if (cursor) return HttpResponse.json({ data: [], nextCursor: null })
        getCount += 1
        return HttpResponse.json({
          data: [
            { id: 200 + getCount, postId: 1, author: { uid: 1, nickname: '테스터' }, content: `조회 ${getCount}`, createdAt: '2026-06-20T12:00:00' },
          ],
          nextCursor: null,
        })
      }),
    )
    const user = userEvent.setup()
    renderWithProviders(<CommunityDetailPage />)

    expect(await screen.findByText('조회 1')).toBeInTheDocument()

    const input = screen.getByPlaceholderText('댓글로 의견을 남겨보세요')
    await user.type(input, '새 댓글')
    await user.click(screen.getByRole('button', { name: '등록' }))

    // invalidate → 재조회로 새 목록 노출
    expect(await screen.findByText('조회 2')).toBeInTheDocument()
  })

  it('내용이 없으면 등록 버튼이 비활성이고 API를 호출하지 않는다', async () => {
    loginAs(1)
    renderWithProviders(<CommunityDetailPage />)

    await screen.findByText('API 본문 내용')

    // 빈 상태에서는 등록 버튼 비활성 (파생 isValid 관례)
    expect(screen.getByRole('button', { name: '등록' })).toBeDisabled()
  })

  it('비로그인 상태면 입력창이 비활성화되고 로그인 안내를 표시한다', async () => {
    renderWithProviders(<CommunityDetailPage />)

    await screen.findByText('API 본문 내용')

    expect(screen.getByPlaceholderText('로그인 후 댓글을 남길 수 있어요')).toBeDisabled()
    expect(screen.getByRole('button', { name: '등록' })).toBeDisabled()
  })

  it('댓글 등록 API 실패 시 에러 메시지를 표시하고 입력값을 유지한다', async () => {
    server.use(
      http.post('http://localhost:5001/api/v1/comments/posts/:postId', () =>
        HttpResponse.json(
          { errorMessage: '서버 오류', errorCode: 'INTERNAL_SERVER_ERROR' },
          { status: 500 },
        ),
      ),
    )
    loginAs(1)
    const user = userEvent.setup()
    renderWithProviders(<CommunityDetailPage />)

    await screen.findByText('API 본문 내용')

    const input = screen.getByPlaceholderText('댓글로 의견을 남겨보세요')
    await user.type(input, '등록 실패할 댓글')
    await user.click(screen.getByRole('button', { name: '등록' }))

    expect(await screen.findByText(/등록에 실패했어요/)).toBeInTheDocument()
    // 실패 시 재시도할 수 있게 입력값 유지
    expect(input).toHaveValue('등록 실패할 댓글')
  })

  it('존재하지 않는 글이면 안내 문구를 표시한다', async () => {
    mockUseParams.mockReturnValue({ postId: '99999' })
    renderWithProviders(<CommunityDetailPage />)

    expect(await screen.findByText('존재하지 않는 글입니다.')).toBeInTheDocument()
  })

  it('서버 오류 시 다시 시도 안내를 표시한다', async () => {
    server.use(
      http.get('http://localhost:5001/api/v1/pms/:section/posts/:id', () =>
        HttpResponse.json(
          { errorMessage: '서버 오류가 발생했습니다.', errorCode: 'INTERNAL_SERVER_ERROR' },
          { status: 500 },
        ),
      ),
    )
    renderWithProviders(<CommunityDetailPage />)

    expect(await screen.findByText(/다시 시도해주세요/)).toBeInTheDocument()
    expect(screen.getByRole('button', { name: '다시 시도' })).toBeInTheDocument()
  })

  it('본인 글이면 수정 버튼이 노출되고 클릭 시 편집 화면으로 이동한다', async () => {
    // id 1 → author.uid 1, 로그인 uid 1 (본인)
    loginAs(1)
    const user = userEvent.setup()
    renderWithProviders(<CommunityDetailPage />)

    const editButton = await screen.findByRole('button', { name: '수정' })
    await user.click(editButton)

    expect(mockNavigate).toHaveBeenCalledWith('/tech/community/1/edit')
  })

  it('타인 글이면 수정 버튼이 노출되지 않는다', async () => {
    // id 2 → author.uid 99, 로그인 uid 1 (타인)
    mockUseParams.mockReturnValue({ postId: '2' })
    loginAs(1)
    renderWithProviders(<CommunityDetailPage />)

    await screen.findByText('API 본문 내용')
    expect(screen.queryByRole('button', { name: '수정' })).not.toBeInTheDocument()
  })

  it('비로그인 상태면 수정 버튼이 노출되지 않는다', async () => {
    renderWithProviders(<CommunityDetailPage />)

    await screen.findByText('API 본문 내용')
    expect(screen.queryByRole('button', { name: '수정' })).not.toBeInTheDocument()
  })

  it('본인 글이면 삭제 버튼이 노출되고, 확인 모달에서 삭제하면 목록으로 이동한다', async () => {
    loginAs(1)
    const user = userEvent.setup()
    renderWithProviders(<CommunityDetailPage />)

    const deleteButton = await screen.findByRole('button', { name: '삭제' })
    await user.click(deleteButton)

    // 확인 모달 노출 후 모달 내 '삭제' 버튼 클릭
    const dialog = await latestDialog()
    await user.click(within(dialog).getByRole('button', { name: '삭제' }))

    await waitFor(() => {
      // 진입 출처로 복귀(뒤로가기와 동일)
      expect(mockNavigate).toHaveBeenCalledWith(-1)
    })
  })

  it('삭제 확인 모달에서 취소하면 삭제되지 않고 이동하지 않는다', async () => {
    loginAs(1)
    const user = userEvent.setup()
    renderWithProviders(<CommunityDetailPage />)

    await user.click(await screen.findByRole('button', { name: '삭제' }))
    const dialog = await latestDialog()
    await user.click(within(dialog).getByRole('button', { name: '취소' }))

    expect(mockNavigate).not.toHaveBeenCalled()
  })

  it('타인 글이면 삭제 버튼이 노출되지 않는다', async () => {
    mockUseParams.mockReturnValue({ postId: '2' })
    loginAs(1)
    renderWithProviders(<CommunityDetailPage />)

    await screen.findByText('API 본문 내용')
    expect(screen.queryByRole('button', { name: '삭제' })).not.toBeInTheDocument()
  })

  it('삭제 API 실패 시 에러 메시지를 표시하고 이동하지 않는다', async () => {
    server.use(
      http.delete('http://localhost:5001/api/v1/pms/:section/posts/:id', () =>
        HttpResponse.json(
          { errorMessage: '서버 오류', errorCode: 'INTERNAL_SERVER_ERROR' },
          { status: 500 },
        ),
      ),
    )
    loginAs(1)
    const user = userEvent.setup()
    renderWithProviders(<CommunityDetailPage />)

    await user.click(await screen.findByRole('button', { name: '삭제' }))
    const dialog = await latestDialog()
    await user.click(within(dialog).getByRole('button', { name: '삭제' }))

    expect(await screen.findByText(/삭제에 실패했어요/)).toBeInTheDocument()
    expect(mockNavigate).not.toHaveBeenCalled()
  })
})
