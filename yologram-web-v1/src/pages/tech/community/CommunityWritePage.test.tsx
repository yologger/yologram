import { describe, it, expect, vi, beforeAll, beforeEach, afterEach, afterAll } from 'vitest'
import { screen, waitFor } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { renderWithProviders } from '../../../test/utils'
import { server } from '../../../test/server'
import CommunityWritePage from './CommunityWritePage'

const mockNavigate = vi.fn()
const mockUseParams = vi.fn()
vi.mock('react-router', async () => {
  const actual = await vi.importActual('react-router')
  return { ...actual, useNavigate: () => mockNavigate, useParams: () => mockUseParams() }
})

beforeAll(() => server.listen())
afterEach(() => {
  server.resetHandlers()
  mockNavigate.mockClear()
})
afterAll(() => server.close())
beforeEach(() => mockUseParams.mockReturnValue({}))

describe('CommunityWritePage', () => {
  it('내용이 비어있으면 남기기 버튼이 비활성화된다', () => {
    renderWithProviders(<CommunityWritePage />)
    expect(screen.getByRole('button', { name: '남기기' })).toBeDisabled()
  })

  it('내용 입력 후 카테고리 선택해 남기기 시 피드로 이동한다', async () => {
    const user = userEvent.setup()
    renderWithProviders(<CommunityWritePage />)

    await user.type(screen.getByPlaceholderText(/광고, 비난/), '테스트 글 내용')
    // 카테고리 로드 후 선택
    await user.click(await screen.findByText('Frontend'))
    await user.click(screen.getByRole('button', { name: '남기기' }))

    await waitFor(() => {
      expect(mockNavigate).toHaveBeenCalledWith('/tech/community')
    })
  })

  it('내용만 입력하고 카테고리 미선택 시 남기기 버튼이 비활성화된다', async () => {
    const user = userEvent.setup()
    renderWithProviders(<CommunityWritePage />)

    await user.type(screen.getByPlaceholderText(/광고, 비난/), '카테고리 미선택 글')
    // 카테고리 로드 후에도 미선택이면 비활성
    await screen.findByText('Frontend')

    expect(screen.getByRole('button', { name: '남기기' })).toBeDisabled()
  })

  describe('수정 모드', () => {
    beforeEach(() => mockUseParams.mockReturnValue({ postId: '1' }))

    it('기존 글의 제목·내용을 prefill하고 수정 버튼을 노출한다', async () => {
      renderWithProviders(<CommunityWritePage />)

      // 기존 글 조회 후 prefill
      expect(await screen.findByDisplayValue('API 제목')).toBeInTheDocument()
      expect(screen.getByDisplayValue('API 본문 내용')).toBeInTheDocument()
      expect(screen.getByRole('button', { name: '수정하기' })).toBeEnabled()
    })

    it('내용 수정 후 수정하기 시 상세로 이동한다', async () => {
      const user = userEvent.setup()
      renderWithProviders(<CommunityWritePage />)

      const contentInput = await screen.findByDisplayValue('API 본문 내용')
      await user.clear(contentInput)
      await user.type(contentInput, '수정된 본문')
      await user.click(screen.getByRole('button', { name: '수정하기' }))

      await waitFor(() => {
        expect(mockNavigate).toHaveBeenCalledWith('/tech/community/1')
      })
    })

    it('내용을 모두 지우면 수정하기 버튼이 비활성화된다', async () => {
      const user = userEvent.setup()
      renderWithProviders(<CommunityWritePage />)

      const contentInput = await screen.findByDisplayValue('API 본문 내용')
      await user.clear(contentInput)

      expect(screen.getByRole('button', { name: '수정하기' })).toBeDisabled()
    })
  })
})
