import { describe, it, expect, vi, beforeAll, afterEach, afterAll, beforeEach } from 'vitest'
import { screen, waitFor } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { http, HttpResponse } from 'msw'
import { QueryClient } from '@tanstack/react-query'
import { getDefaultStore } from 'jotai'
import { renderWithProviders } from '../../../../../../test/utils'
import { server } from '../../../../../../test/server'
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
      http.get('http://localhost:5002/api/v2/comments/:section/posts/:postId', ({ request }) => {
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
      http.get('http://localhost:5002/api/v2/comments/:section/posts/:postId', () =>
        HttpResponse.json({ data: [], nextCursor: null }),
      ),
    )
    renderWithProviders(<CommunityDetail />)

    await screen.findByText('API 본문 내용')
    expect(await screen.findByText('첫 댓글을 남겨보세요.')).toBeInTheDocument()
  })

  it('댓글 조회 실패 시 에러 안내를 표시한다', async () => {
    server.use(
      http.get('http://localhost:5002/api/v2/comments/:section/posts/:postId', () =>
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
      http.post('http://localhost:5002/api/v2/comments/:section/posts/:postId', () => {
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
      http.get('http://localhost:5002/api/v2/comments/:section/posts/:postId', () => {
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
      http.post('http://localhost:5002/api/v2/comments/:section/posts/:postId', () => {
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
      http.post('http://localhost:5002/api/v2/comments/:section/posts/:postId', () =>
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

  it('글 삭제 성공 시 상세·댓글 캐시를 제거한다', async () => {
    const removeSpy = vi.spyOn(QueryClient.prototype, 'removeQueries')
    store.set(authAtom, { uid: 12, email: 't@yologram.link', name: '테스터', nickname: 'tester', accessToken: 't' })
    const user = userEvent.setup()
    renderWithProviders(<CommunityDetail />)

    await screen.findByText('API 본문 내용')
    await user.click(await screen.findByRole('button', { name: '삭제' }))

    await screen.findAllByText('글을 삭제할까요?')
    const okButton = document.querySelector('.ant-modal-confirm-btns .ant-btn-dangerous') as HTMLElement
    await user.click(okButton)

    await waitFor(() => {
      expect(removeSpy).toHaveBeenCalledWith({ queryKey: ['post', 'tech', 1] })
      expect(removeSpy).toHaveBeenCalledWith({ queryKey: ['comments', 'tech', 1] })
    })
    removeSpy.mockRestore()
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

  it('본인 댓글에만 수정 버튼이 보이고 타인 댓글에는 보이지 않는다', async () => {
    // 기본 핸들러: 댓글 401(uid 1, 최신유저), 400(uid 2, 이전유저). uid 1로 로그인
    store.set(authAtom, { uid: 1, email: 't@yologram.link', name: '테스터', nickname: 'tester', accessToken: 't' })
    renderWithProviders(<CommunityDetail />)

    expect(await screen.findByText('가장 최근 댓글')).toBeInTheDocument()
    // 본인 댓글(uid 1) 하나에만 수정 버튼 노출
    expect(screen.getAllByRole('button', { name: '댓글 수정' })).toHaveLength(1)
  })

  it('비로그인 상태에서는 댓글 수정 버튼이 보이지 않는다', async () => {
    renderWithProviders(<CommunityDetail />)

    await screen.findByText('가장 최근 댓글')
    expect(screen.queryByRole('button', { name: '댓글 수정' })).not.toBeInTheDocument()
  })

  it('본인 댓글을 인라인 편집·저장하면 PATCH 요청·목록 재조회·성공 피드백이 이뤄진다', async () => {
    let patched: { commentId: string; content: string } | null = null
    let getCount = 0
    server.use(
      http.patch('http://localhost:5002/api/v2/comments/:section/:commentId', async ({ request, params }) => {
        const body = await request.json() as { content: string }
        patched = { commentId: String(params.commentId), content: body.content }
        return new HttpResponse(null, { status: 204 })
      }),
      http.get('http://localhost:5002/api/v2/comments/:section/posts/:postId', () => {
        getCount += 1
        return HttpResponse.json({
          data: [{ id: 401, postId: 1, author: { uid: 1, nickname: '최신유저' }, content: '가장 최근 댓글', createdAt: '2026-06-10T12:00:00' }],
          nextCursor: null,
        })
      }),
    )
    store.set(authAtom, { uid: 1, email: 't@yologram.link', name: '테스터', nickname: 'tester', accessToken: 't' })
    const user = userEvent.setup()
    renderWithProviders(<CommunityDetail />)

    await screen.findByText('가장 최근 댓글')
    const initialGetCount = getCount

    await user.click(screen.getByRole('button', { name: '댓글 수정' }))

    // 편집모드: 기존 내용이 채워진 textarea 노출
    const textarea = screen.getByDisplayValue('가장 최근 댓글') as HTMLTextAreaElement
    await user.clear(textarea)
    await user.type(textarea, '수정된 댓글')
    await user.click(screen.getByRole('button', { name: '저장' }))

    expect(await screen.findByText('댓글이 수정되었습니다.')).toBeInTheDocument()
    await waitFor(() => expect(patched).toEqual({ commentId: '401', content: '수정된 댓글' }))
    // 수정 성공 시 invalidateQueries로 목록 GET 재요청 발생
    await waitFor(() => expect(getCount).toBeGreaterThan(initialGetCount))
    // 편집 종료(textarea 사라짐)
    await waitFor(() => expect(screen.queryByRole('button', { name: '저장' })).not.toBeInTheDocument())
  })

  it('편집 중 취소하면 요청 없이 원래 내용으로 돌아간다', async () => {
    let patchCalled = false
    server.use(
      http.patch('http://localhost:5002/api/v2/comments/:section/:commentId', () => {
        patchCalled = true
        return new HttpResponse(null, { status: 204 })
      }),
    )
    store.set(authAtom, { uid: 1, email: 't@yologram.link', name: '테스터', nickname: 'tester', accessToken: 't' })
    const user = userEvent.setup()
    renderWithProviders(<CommunityDetail />)

    await screen.findByText('가장 최근 댓글')
    await user.click(screen.getByRole('button', { name: '댓글 수정' }))

    const textarea = screen.getByDisplayValue('가장 최근 댓글') as HTMLTextAreaElement
    await user.clear(textarea)
    await user.type(textarea, '취소될 수정')
    await user.click(screen.getByRole('button', { name: '취소' }))

    // 편집 종료 + 원본 내용 유지 + 요청 미발생
    await waitFor(() => expect(screen.queryByRole('button', { name: '저장' })).not.toBeInTheDocument())
    expect(screen.getByText('가장 최근 댓글')).toBeInTheDocument()
    expect(patchCalled).toBe(false)
  })

  it('편집 내용이 비면 저장 버튼이 비활성화된다', async () => {
    store.set(authAtom, { uid: 1, email: 't@yologram.link', name: '테스터', nickname: 'tester', accessToken: 't' })
    const user = userEvent.setup()
    renderWithProviders(<CommunityDetail />)

    await screen.findByText('가장 최근 댓글')
    await user.click(screen.getByRole('button', { name: '댓글 수정' }))

    const textarea = screen.getByDisplayValue('가장 최근 댓글') as HTMLTextAreaElement
    await user.clear(textarea)

    expect(screen.getByRole('button', { name: '저장' })).toBeDisabled()
  })

  it('본인 댓글에만 삭제 버튼이 보이고 타인 댓글에는 보이지 않는다', async () => {
    // 기본 핸들러: 댓글 401(uid 1), 400(uid 2). uid 1로 로그인 → 본인 댓글 하나만 삭제 버튼 노출
    store.set(authAtom, { uid: 1, email: 't@yologram.link', name: '테스터', nickname: 'tester', accessToken: 't' })
    renderWithProviders(<CommunityDetail />)

    expect(await screen.findByText('가장 최근 댓글')).toBeInTheDocument()
    expect(screen.getAllByRole('button', { name: '댓글 삭제' })).toHaveLength(1)
  })

  it('비로그인 상태에서는 댓글 삭제 버튼이 보이지 않는다', async () => {
    renderWithProviders(<CommunityDetail />)

    await screen.findByText('가장 최근 댓글')
    expect(screen.queryByRole('button', { name: '댓글 삭제' })).not.toBeInTheDocument()
  })

  it('본인 댓글 삭제 확인 시 DELETE 요청·목록 재조회·성공 피드백이 이뤄진다', async () => {
    let deleted: string | null = null
    let getCount = 0
    server.use(
      http.delete('http://localhost:5002/api/v2/comments/:section/:commentId', ({ params }) => {
        deleted = String(params.commentId)
        return new HttpResponse(null, { status: 204 })
      }),
      http.get('http://localhost:5002/api/v2/comments/:section/posts/:postId', () => {
        getCount += 1
        return HttpResponse.json({
          data: [{ id: 401, postId: 1, author: { uid: 1, nickname: '최신유저' }, content: '가장 최근 댓글', createdAt: '2026-06-10T12:00:00' }],
          nextCursor: null,
        })
      }),
    )
    store.set(authAtom, { uid: 1, email: 't@yologram.link', name: '테스터', nickname: 'tester', accessToken: 't' })
    const user = userEvent.setup()
    renderWithProviders(<CommunityDetail />)

    await screen.findByText('가장 최근 댓글')
    const initialGetCount = getCount

    await user.click(screen.getByRole('button', { name: '댓글 삭제' }))

    // 확인 모달 노출
    expect((await screen.findAllByText('댓글을 삭제할까요?')).length).toBeGreaterThan(0)

    const okButton = document.querySelector('.ant-modal-confirm-btns .ant-btn-dangerous') as HTMLElement
    await user.click(okButton)

    expect(await screen.findByText('댓글이 삭제되었습니다.')).toBeInTheDocument()
    await waitFor(() => expect(deleted).toBe('401'))
    // 삭제 성공 시 invalidateQueries로 목록 GET 재요청 발생
    await waitFor(() => expect(getCount).toBeGreaterThan(initialGetCount))
  })

  it('삭제 확인 모달에서 취소하면 DELETE 요청이 발생하지 않는다', async () => {
    let deleteCalled = false
    server.use(
      http.delete('http://localhost:5002/api/v2/comments/:section/:commentId', () => {
        deleteCalled = true
        return new HttpResponse(null, { status: 204 })
      }),
    )
    store.set(authAtom, { uid: 1, email: 't@yologram.link', name: '테스터', nickname: 'tester', accessToken: 't' })
    const user = userEvent.setup()
    renderWithProviders(<CommunityDetail />)

    await screen.findByText('가장 최근 댓글')
    await user.click(screen.getByRole('button', { name: '댓글 삭제' }))

    await screen.findAllByText('댓글을 삭제할까요?')
    const cancelButton = document.querySelector('.ant-modal-confirm-btns .ant-btn:not(.ant-btn-dangerous)') as HTMLElement
    await user.click(cancelButton)

    await new Promise((r) => setTimeout(r, 100))
    expect(deleteCalled).toBe(false)
  })

  it('댓글 삭제 실패 시 에러 토스트를 표시한다', async () => {
    server.use(
      http.delete('http://localhost:5002/api/v2/comments/:section/:commentId', () =>
        HttpResponse.json(
          { errorMessage: '서버 오류가 발생했습니다.', errorCode: 'INTERNAL_SERVER_ERROR' },
          { status: 500 },
        ),
      ),
    )
    store.set(authAtom, { uid: 1, email: 't@yologram.link', name: '테스터', nickname: 'tester', accessToken: 't' })
    const user = userEvent.setup()
    renderWithProviders(<CommunityDetail />)

    await screen.findByText('가장 최근 댓글')
    await user.click(screen.getByRole('button', { name: '댓글 삭제' }))

    await screen.findAllByText('댓글을 삭제할까요?')
    const okButton = document.querySelector('.ant-modal-confirm-btns .ant-btn-dangerous') as HTMLElement
    await user.click(okButton)

    expect(await screen.findByText(/댓글 삭제에 실패했어요/)).toBeInTheDocument()
  })

  it('댓글 수정 실패 시 에러 토스트를 표시한다', async () => {
    server.use(
      http.patch('http://localhost:5002/api/v2/comments/:section/:commentId', () =>
        HttpResponse.json(
          { errorMessage: '서버 오류가 발생했습니다.', errorCode: 'INTERNAL_SERVER_ERROR' },
          { status: 500 },
        ),
      ),
    )
    store.set(authAtom, { uid: 1, email: 't@yologram.link', name: '테스터', nickname: 'tester', accessToken: 't' })
    const user = userEvent.setup()
    renderWithProviders(<CommunityDetail />)

    await screen.findByText('가장 최근 댓글')
    await user.click(screen.getByRole('button', { name: '댓글 수정' }))

    const textarea = screen.getByDisplayValue('가장 최근 댓글') as HTMLTextAreaElement
    await user.clear(textarea)
    await user.type(textarea, '실패할 수정')
    await user.click(screen.getByRole('button', { name: '저장' }))

    expect(await screen.findByText(/댓글 수정에 실패했어요/)).toBeInTheDocument()
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
