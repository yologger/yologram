import { describe, it, expect, vi, beforeAll, afterEach, afterAll, beforeEach } from 'vitest'
import { screen, waitFor } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { http, HttpResponse } from 'msw'
import { getDefaultStore } from 'jotai'
import { renderWithProviders } from '../../../../../test/utils'
import { server } from '../../../../../test/server'
import { authAtom } from '@/stores/auth'
import CommunityDetail from './page'

const mockPush = vi.fn()
const mockBack = vi.fn()
const mockUseParams = vi.fn()
vi.mock('next/navigation', () => ({
  useRouter: () => ({ back: mockBack, push: mockPush }),
  useParams: () => mockUseParams(),
}))

const store = getDefaultStore()

// IntersectionObserver stub — observe 콜백을 캡처해 무한스크롤을 수동 트리거할 수 있게 함
let ioCallback: IntersectionObserverCallback | null = null
beforeAll(() => {
  server.listen()
  vi.stubGlobal(
    'IntersectionObserver',
    class {
      constructor(cb: IntersectionObserverCallback) {
        ioCallback = cb
      }
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
  mockBack.mockClear()
})
afterAll(() => server.close())
beforeEach(() => mockUseParams.mockReturnValue({ postId: '1' }))

describe('CommunityDetail', () => {
  it('API로 조회한 게시글 본문과 댓글 입력을 표시한다', async () => {
    renderWithProviders(<CommunityDetail />)

    expect(await screen.findByText('API 본문 내용')).toBeInTheDocument()
    expect(screen.getByText('테스터')).toBeInTheDocument()
    expect(screen.getByPlaceholderText('댓글로 의견을 남겨보세요')).toBeInTheDocument()
  })

  it('API로 조회한 댓글 목록(닉네임/내용)을 렌더링한다', async () => {
    renderWithProviders(<CommunityDetail />)

    expect(await screen.findByText('가장 최근 댓글')).toBeInTheDocument()
    expect(screen.getByText('최신유저')).toBeInTheDocument()
    expect(screen.getByText('조금 이전 댓글')).toBeInTheDocument()
  })

  it('정렬을 오래된순으로 전환하면 해당 정렬로 재조회한다', async () => {
    const sortValues: string[] = []
    server.use(
      http.get('http://localhost:5002/api/v2/comments/posts/:postId', ({ request }) => {
        const sort = new URL(request.url).searchParams.get('sort') ?? 'latest'
        sortValues.push(sort)
        return HttpResponse.json({
          data: [
            sort === 'oldest'
              ? { id: 400, postId: 1, author: { uid: 2, nickname: '이전유저' }, content: '가장 오래된 댓글', createdAt: '2026-06-09T12:00:00' }
              : { id: 401, postId: 1, author: { uid: 1, nickname: '최신유저' }, content: '가장 최근 댓글', createdAt: '2026-06-10T12:00:00' },
          ],
          nextCursor: null,
        })
      }),
    )
    const user = userEvent.setup()
    renderWithProviders(<CommunityDetail />)

    expect(await screen.findByText('가장 최근 댓글')).toBeInTheDocument()

    await user.click(screen.getByRole('button', { name: '오래된순' }))

    expect(await screen.findByText('가장 오래된 댓글')).toBeInTheDocument()
    await waitFor(() => expect(sortValues).toContain('oldest'))
  })

  it('sentinel 교차 시 다음 페이지를 불러온다(무한스크롤)', async () => {
    renderWithProviders(<CommunityDetail />)

    await screen.findByText('가장 최근 댓글')

    // IntersectionObserver 콜백을 수동 트리거해 fetchNextPage 유발
    ioCallback?.([{ isIntersecting: true } as IntersectionObserverEntry], {} as IntersectionObserver)

    expect(await screen.findByText('커서 이후 댓글')).toBeInTheDocument()
  })

  it('댓글이 없으면 안내 문구를 표시한다', async () => {
    server.use(
      http.get('http://localhost:5002/api/v2/comments/posts/:postId', () =>
        HttpResponse.json({ data: [], nextCursor: null }),
      ),
    )
    renderWithProviders(<CommunityDetail />)

    await screen.findByText('API 본문 내용')
    expect(await screen.findByText('첫 댓글을 남겨보세요.')).toBeInTheDocument()
  })

  it('댓글 조회 실패 시 에러 안내를 표시한다', async () => {
    server.use(
      http.get('http://localhost:5002/api/v2/comments/posts/:postId', () =>
        HttpResponse.json(
          { errorMessage: '서버 오류가 발생했습니다.', errorCode: 'INTERNAL_SERVER_ERROR' },
          { status: 500 },
        ),
      ),
    )
    renderWithProviders(<CommunityDetail />)

    await screen.findByText('API 본문 내용')
    expect(await screen.findByText('댓글을 불러오지 못했어요.')).toBeInTheDocument()
  })

  it('내용이 없으면 등록 버튼이 비활성화되고 클릭해도 요청하지 않는다', async () => {
    let called = false
    server.use(
      http.post('http://localhost:5002/api/v2/comments/posts/:postId', () => {
        called = true
        return HttpResponse.json({ data: { id: 7777 } }, { status: 201 })
      }),
    )
    store.set(authAtom, { uid: 12, email: 't@yologram.link', name: '테스터', nickname: 'tester', accessToken: 't' })
    renderWithProviders(<CommunityDetail />)

    await screen.findByText('API 본문 내용')
    expect(screen.getByRole('button', { name: '등록' })).toBeDisabled()

    await new Promise((r) => setTimeout(r, 50))
    expect(called).toBe(false)
  })

  it('로그인 상태에서 댓글을 입력하고 등록하면 성공 피드백·입력창 초기화·목록 재조회가 이뤄진다', async () => {
    let getCount = 0
    server.use(
      http.get('http://localhost:5002/api/v2/comments/posts/:postId', () => {
        getCount += 1
        return HttpResponse.json({
          data: [{ id: 401, postId: 1, author: { uid: 1, nickname: '최신유저' }, content: '가장 최근 댓글', createdAt: '2026-06-10T12:00:00' }],
          nextCursor: null,
        })
      }),
    )
    store.set(authAtom, { uid: 12, email: 't@yologram.link', name: '테스터', nickname: 'tester', accessToken: 't' })
    const user = userEvent.setup()
    renderWithProviders(<CommunityDetail />)

    await screen.findByText('가장 최근 댓글')
    const initialGetCount = getCount

    const input = screen.getByPlaceholderText('댓글로 의견을 남겨보세요') as HTMLInputElement
    await user.type(input, '좋은 글이네요')
    await user.click(screen.getByRole('button', { name: '등록' }))

    expect(await screen.findByText('댓글이 등록되었습니다.')).toBeInTheDocument()
    await waitFor(() => expect(input.value).toBe(''))
    // 작성 성공 시 invalidateQueries로 목록 GET 재요청 발생
    await waitFor(() => expect(getCount).toBeGreaterThan(initialGetCount))
  })

  it('비로그인 상태에서 등록하면 로그인 안내만 하고 요청하지 않는다', async () => {
    let called = false
    server.use(
      http.post('http://localhost:5002/api/v2/comments/posts/:postId', () => {
        called = true
        return HttpResponse.json({ data: { id: 7777 } }, { status: 201 })
      }),
    )
    // 비로그인 상태 유지(afterEach에서 null 설정)
    const user = userEvent.setup()
    renderWithProviders(<CommunityDetail />)

    await screen.findByText('API 본문 내용')
    await user.type(screen.getByPlaceholderText('댓글로 의견을 남겨보세요'), '댓글 내용')
    await user.click(screen.getByRole('button', { name: '등록' }))

    expect(await screen.findByText('로그인 후 댓글을 남길 수 있어요.')).toBeInTheDocument()
    expect(called).toBe(false)
  })

  it('댓글 등록 실패 시 에러 토스트를 표시한다', async () => {
    server.use(
      http.post('http://localhost:5002/api/v2/comments/posts/:postId', () =>
        HttpResponse.json(
          { errorMessage: '서버 오류가 발생했습니다.', errorCode: 'INTERNAL_SERVER_ERROR' },
          { status: 500 },
        ),
      ),
    )
    store.set(authAtom, { uid: 12, email: 't@yologram.link', name: '테스터', nickname: 'tester', accessToken: 't' })
    const user = userEvent.setup()
    renderWithProviders(<CommunityDetail />)

    await screen.findByText('API 본문 내용')
    await user.type(screen.getByPlaceholderText('댓글로 의견을 남겨보세요'), '실패할 댓글')
    await user.click(screen.getByRole('button', { name: '등록' }))

    expect(await screen.findByText(/댓글 등록에 실패했어요/)).toBeInTheDocument()
  })

  it('존재하지 않는 글이면 안내 문구를 표시한다', async () => {
    mockUseParams.mockReturnValue({ postId: '99999' })
    renderWithProviders(<CommunityDetail />)

    expect(await screen.findByText('존재하지 않는 글입니다.')).toBeInTheDocument()
  })

  it('비로그인 또는 타인 글이면 수정 버튼이 보이지 않는다', async () => {
    // 핸들러의 author.uid는 12. 로그인하지 않으면 수정 버튼 숨김
    renderWithProviders(<CommunityDetail />)

    await screen.findByText('API 본문 내용')
    expect(screen.queryByRole('button', { name: '수정' })).not.toBeInTheDocument()
  })

  it('본인 글이면 수정 버튼이 보이고 클릭 시 수정 화면으로 이동한다', async () => {
    store.set(authAtom, { uid: 12, email: 't@yologram.link', name: '테스터', nickname: 'tester', accessToken: 't' })
    const user = userEvent.setup()
    renderWithProviders(<CommunityDetail />)

    await screen.findByText('API 본문 내용')
    const editButton = await screen.findByRole('button', { name: '수정' })
    await user.click(editButton)

    expect(mockPush).toHaveBeenCalledWith('/tech/community/1/edit')
  })

  it('비로그인 또는 타인 글이면 삭제 버튼이 보이지 않는다', async () => {
    renderWithProviders(<CommunityDetail />)

    await screen.findByText('API 본문 내용')
    expect(screen.queryByRole('button', { name: '삭제' })).not.toBeInTheDocument()
  })

  it('본인 글에서 삭제 확인 시 삭제 후 목록으로 이동한다', async () => {
    store.set(authAtom, { uid: 12, email: 't@yologram.link', name: '테스터', nickname: 'tester', accessToken: 't' })
    const user = userEvent.setup()
    renderWithProviders(<CommunityDetail />)

    await screen.findByText('API 본문 내용')
    await user.click(await screen.findByRole('button', { name: '삭제' }))

    // 확인 모달 노출
    expect((await screen.findAllByText('글을 삭제할까요?')).length).toBeGreaterThan(0)

    // 모달 확인(삭제) 버튼은 danger 스타일
    const okButton = document.querySelector('.ant-modal-confirm-btns .ant-btn-dangerous') as HTMLElement
    await user.click(okButton)

    await waitFor(() => {
      // 진입 출처로 복귀(뒤로가기와 동일)
      expect(mockBack).toHaveBeenCalled()
    })
  })

  it('삭제 확인 모달에서 취소하면 삭제되지 않는다', async () => {
    store.set(authAtom, { uid: 12, email: 't@yologram.link', name: '테스터', nickname: 'tester', accessToken: 't' })
    const user = userEvent.setup()
    renderWithProviders(<CommunityDetail />)

    await screen.findByText('API 본문 내용')
    await user.click(await screen.findByRole('button', { name: '삭제' }))

    await screen.findAllByText('글을 삭제할까요?')
    const cancelButton = document.querySelector('.ant-modal-confirm-btns .ant-btn:not(.ant-btn-dangerous)') as HTMLElement
    await user.click(cancelButton)

    // 취소 시 목록 이동 미발생
    await new Promise((r) => setTimeout(r, 100))
    expect(mockBack).not.toHaveBeenCalled()
  })

  it('서버 오류 시 다시 시도 안내를 표시한다', async () => {
    server.use(
      http.get('http://localhost:5002/api/v2/pms/:section/posts/:id', () =>
        HttpResponse.json(
          { errorMessage: '서버 오류가 발생했습니다.', errorCode: 'INTERNAL_SERVER_ERROR' },
          { status: 500 },
        ),
      ),
    )
    renderWithProviders(<CommunityDetail />)

    expect(await screen.findByText(/다시 시도해주세요/)).toBeInTheDocument()
    expect(screen.getByRole('button', { name: '다시 시도' })).toBeInTheDocument()
  })
})
