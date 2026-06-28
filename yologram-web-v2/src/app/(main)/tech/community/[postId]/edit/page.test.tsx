import { describe, it, expect, vi, beforeAll, beforeEach, afterEach, afterAll } from 'vitest'
import { screen, waitFor } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { http, HttpResponse } from 'msw'
import { getDefaultStore } from 'jotai'
import { renderWithProviders } from '../../../../../../test/utils'
import { server } from '../../../../../../test/server'
import { authAtom } from '@/stores/auth'
import CommunityEdit from './page'

const mockPush = vi.fn()
const mockReplace = vi.fn()
const mockUseParams = vi.fn()
vi.mock('next/navigation', () => ({
  useRouter: () => ({ push: mockPush, replace: mockReplace, back: vi.fn() }),
  useParams: () => mockUseParams(),
}))

const store = getDefaultStore()
// 상세 핸들러는 author.uid: 12 를 반환 → 본인으로 판정되도록 uid 12 로그인
const owner = { uid: 12, email: 't@yologram.link', name: '테스터', nickname: 'tester', accessToken: 't' }

beforeAll(() => server.listen())
beforeEach(() => {
  mockUseParams.mockReturnValue({ postId: '1' })
  store.set(authAtom, owner)
})
afterEach(() => {
  server.resetHandlers()
  store.set(authAtom, null)
  mockPush.mockClear()
  mockReplace.mockClear()
})
afterAll(() => server.close())

describe('CommunityEdit', () => {
  it('기존 글의 제목·내용·카테고리를 prefill한다', async () => {
    renderWithProviders(<CommunityEdit />)

    expect(await screen.findByDisplayValue('API 제목')).toBeInTheDocument()
    expect(screen.getByDisplayValue('API 본문 내용')).toBeInTheDocument()
    // categoryIds [1] = Frontend 가 선택 상태
    await waitFor(() => {
      expect(screen.getByText('Frontend').className).toMatch(/active/)
    })
  })

  it('내용을 수정하고 수정 버튼을 누르면 상세로 이동한다', async () => {
    const user = userEvent.setup()
    renderWithProviders(<CommunityEdit />)

    const textarea = await screen.findByDisplayValue('API 본문 내용')
    await user.clear(textarea)
    await user.type(textarea, '수정된 내용')
    await user.click(screen.getByRole('button', { name: '수정' }))

    await waitFor(() => {
      expect(mockPush).toHaveBeenCalledWith('/tech/community/1')
    })
  })

  it('내용을 비우면 수정 버튼이 비활성화된다', async () => {
    const user = userEvent.setup()
    renderWithProviders(<CommunityEdit />)

    const textarea = await screen.findByDisplayValue('API 본문 내용')
    await user.clear(textarea)

    expect(screen.getByRole('button', { name: '수정' })).toBeDisabled()
  })

  it('본인 글이 아니면 상세로 되돌린다', async () => {
    // 다른 uid로 로그인 → 비소유자
    store.set(authAtom, { ...owner, uid: 999 })
    renderWithProviders(<CommunityEdit />)

    await waitFor(() => {
      expect(mockReplace).toHaveBeenCalledWith('/tech/community/1')
    })
  })

  it('존재하지 않는 글이면 안내 문구를 표시한다', async () => {
    mockUseParams.mockReturnValue({ postId: '99999' })
    renderWithProviders(<CommunityEdit />)

    expect(await screen.findByText('존재하지 않는 글입니다.')).toBeInTheDocument()
  })

  it('수정 실패 시 에러 메시지를 표시하고 이동하지 않는다', async () => {
    server.use(
      http.patch('http://localhost:5002/api/v2/pms/:section/posts/:id', () =>
        HttpResponse.json(
          { errorMessage: '권한이 없습니다.', errorCode: 'POST_FORBIDDEN' },
          { status: 403 },
        ),
      ),
    )
    const user = userEvent.setup()
    renderWithProviders(<CommunityEdit />)

    const textarea = await screen.findByDisplayValue('API 본문 내용')
    await user.clear(textarea)
    await user.type(textarea, '수정 시도')
    await user.click(screen.getByRole('button', { name: '수정' }))

    expect(await screen.findByText('글 수정에 실패했어요. 잠시 후 다시 시도해주세요.')).toBeInTheDocument()
    expect(mockPush).not.toHaveBeenCalled()
  })
})
