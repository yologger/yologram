import { describe, it, expect, vi, beforeAll, afterEach, afterAll, beforeEach } from 'vitest'
import { screen, waitFor } from '@testing-library/react'
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

beforeAll(() => server.listen())
afterEach(() => {
  server.resetHandlers()
  mockNavigate.mockClear()
  getDefaultStore().set(authAtom, null)
})
afterAll(() => server.close())
beforeEach(() => mockUseParams.mockReturnValue({ postId: '1' }))

describe('CommunityDetailPage', () => {
  it('API로 조회한 게시글 본문과 댓글 영역을 표시한다', async () => {
    renderWithProviders(<CommunityDetailPage />)

    expect(await screen.findByText('API 본문 내용')).toBeInTheDocument()
    expect(screen.getByText('테스터')).toBeInTheDocument()
    expect(screen.getByPlaceholderText('댓글로 의견을 남겨보세요')).toBeInTheDocument()
  })

  it('댓글을 입력하고 등록하면 목록에 추가된다', async () => {
    const user = userEvent.setup()
    renderWithProviders(<CommunityDetailPage />)

    // 게시글 로드 대기
    await screen.findByText('API 본문 내용')

    await user.type(screen.getByPlaceholderText('댓글로 의견을 남겨보세요'), '좋은 글이네요')
    await user.click(screen.getByRole('button', { name: '등록' }))

    await waitFor(() => {
      expect(screen.getByText('좋은 글이네요')).toBeInTheDocument()
    })
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
})
