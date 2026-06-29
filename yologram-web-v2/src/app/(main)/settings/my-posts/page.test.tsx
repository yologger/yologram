import { describe, it, expect, vi, beforeAll, beforeEach, afterEach, afterAll } from 'vitest'
import { screen, waitFor } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { http, HttpResponse } from 'msw'
import { getDefaultStore } from 'jotai'
import { renderWithProviders } from '../../../../test/utils'
import { server } from '../../../../test/server'
import { authAtom } from '@/stores/auth'
import MyPosts from './page'

const mockPush = vi.fn()
vi.mock('next/navigation', () => ({
  useRouter: () => ({ push: mockPush, replace: vi.fn() }),
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
beforeEach(() => {
  store.set(authAtom, { uid: 1, email: 't@yologram.link', name: '테스터', nickname: 'tester', accessToken: 't' })
})
afterEach(() => {
  server.resetHandlers()
  store.set(authAtom, null)
  mockPush.mockClear()
})
afterAll(() => server.close())

describe('MyPosts 내가 쓴 글', () => {
  it('기본(기술) 탭의 내 글이 렌더링된다', async () => {
    renderWithProviders(<MyPosts />)

    expect(await screen.findByText('내 기술 글 1')).toBeInTheDocument()
  })

  it('탭 변경 시 해당 섹션의 내 글만 조회한다', async () => {
    const user = userEvent.setup()
    renderWithProviders(<MyPosts />)

    await screen.findByText('내 기술 글 1')

    await user.click(screen.getByText('투자'))

    expect(await screen.findByText('내 투자 글 1')).toBeInTheDocument()
    expect(screen.queryByText('내 기술 글 1')).not.toBeInTheDocument()
  })

  it('기술 글 클릭 시 상세 페이지로 이동한다', async () => {
    const user = userEvent.setup()
    renderWithProviders(<MyPosts />)

    await user.click(await screen.findByText('내 기술 글 1'))

    expect(mockPush).toHaveBeenCalledWith('/tech/community/3001')
  })

  it('글이 없으면 빈 상태 메시지를 표시한다', async () => {
    server.use(
      http.get('http://localhost:5002/api/v2/pms/posts/me', () =>
        HttpResponse.json({ data: [], nextCursor: null }),
      ),
    )

    renderWithProviders(<MyPosts />)

    expect(await screen.findByText('작성한 글이 없어요')).toBeInTheDocument()
  })

  it('삭제 버튼 클릭 후 확인하면 해당 글의 DELETE를 호출한다', async () => {
    let deletedPath: string | null = null
    server.use(
      http.delete('http://localhost:5002/api/v2/pms/:section/posts/:id', ({ params }) => {
        deletedPath = `${params.section}/${params.id}`
        return new HttpResponse(null, { status: 204 })
      }),
    )

    const user = userEvent.setup()
    renderWithProviders(<MyPosts />)

    await screen.findByText('내 기술 글 1')

    // 첫 카드(기술 글, id 3001, section TECH)의 삭제 버튼
    const deleteButtons = screen.getAllByRole('button', { name: '삭제' })
    await user.click(deleteButtons[0])

    // 확인 모달 노출
    expect((await screen.findAllByText('글을 삭제할까요?')).length).toBeGreaterThan(0)

    // 모달 확인(삭제) 버튼은 danger 스타일
    const okButton = document.querySelector('.ant-modal-confirm-btns .ant-btn-dangerous') as HTMLElement
    await user.click(okButton)

    await waitFor(() => {
      expect(deletedPath).toBe('tech/3001')
    })
  })

  it('삭제 확인 모달에서 취소하면 DELETE를 호출하지 않는다', async () => {
    let called = false
    server.use(
      http.delete('http://localhost:5002/api/v2/pms/:section/posts/:id', () => {
        called = true
        return new HttpResponse(null, { status: 204 })
      }),
    )

    const user = userEvent.setup()
    renderWithProviders(<MyPosts />)

    await screen.findByText('내 기술 글 1')
    await user.click(screen.getAllByRole('button', { name: '삭제' })[0])

    await screen.findAllByText('글을 삭제할까요?')
    const cancelButton = document.querySelector('.ant-modal-confirm-btns .ant-btn:not(.ant-btn-dangerous)') as HTMLElement
    await user.click(cancelButton)

    // 취소 시 DELETE 미호출
    await new Promise((r) => setTimeout(r, 100))
    expect(called).toBe(false)
  })

  it('조회 실패 시 에러 메시지와 다시 시도 버튼을 표시한다', async () => {
    server.use(
      http.get('http://localhost:5002/api/v2/pms/posts/me', () =>
        HttpResponse.json(
          { errorMessage: '서버 오류', errorCode: 'INTERNAL_ERROR' },
          { status: 500 },
        ),
      ),
    )

    renderWithProviders(<MyPosts />)

    expect(await screen.findByText(/글을 불러오지 못했어요/)).toBeInTheDocument()
    expect(screen.getByRole('button', { name: '다시 시도' })).toBeInTheDocument()
  })
})
