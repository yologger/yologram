import { describe, it, expect, beforeAll, beforeEach, afterEach, afterAll } from 'vitest'
import { screen, waitFor, within } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { getDefaultStore } from 'jotai'
import { http, HttpResponse } from 'msw'
import { server } from '../../test/server'
import { renderWithProviders } from '../../test/utils'
import { authAtom, type AuthState } from '../../stores/auth'
import NewsSourcesPage from './NewsSourcesPage'

const adminAuth: AuthState = {
  uid: 1,
  accessToken: 'valid-token',
  email: 'admin@yologram.link',
  name: '관리자',
  role: 'OWNER',
}

const BASE_URL = 'http://localhost:5001/api/v1/news/admin/tech/sources'

beforeAll(() => server.listen())
beforeEach(() => {
  getDefaultStore().set(authAtom, adminAuth)
})
afterEach(() => {
  server.resetHandlers()
  getDefaultStore().set(authAtom, null)
  localStorage.removeItem('auth')
})
afterAll(() => server.close())

function renderPage() {
  return renderWithProviders(<NewsSourcesPage />)
}

/** modal.confirm(삭제 확인)은 body portal에 렌더된다 */
async function findConfirmModal() {
  await waitFor(() => {
    expect(document.querySelector('.ant-modal-confirm')).toBeInTheDocument()
  })
  return document.querySelector('.ant-modal-confirm') as HTMLElement
}

describe('NewsSourcesPage (목록)', () => {
  it('타이틀·소스 추가 버튼과 소스 목록 테이블을 렌더한다', async () => {
    renderPage()

    expect(screen.getByRole('heading', { level: 3, name: '소스 관리' })).toBeInTheDocument()
    expect(screen.getByRole('button', { name: /소스 추가/ })).toBeInTheDocument()

    expect(await screen.findByText('우아한형제들 기술블로그')).toBeInTheDocument()
    expect(screen.getByText('카카오 기술블로그')).toBeInTheDocument()
    expect(screen.getByRole('link', { name: 'https://techblog.woowahan.com/feed' })).toHaveAttribute(
      'href',
      'https://techblog.woowahan.com/feed',
    )
    expect(screen.getByText('2026-07-01')).toBeInTheDocument()

    const switches = screen.getAllByRole('switch')
    expect(switches).toHaveLength(2)
    expect(switches[0]).toBeChecked()
    expect(switches[1]).not.toBeChecked()
  })

  it('빈 목록이면 안내 문구를 렌더한다', async () => {
    server.use(http.get(BASE_URL, () => HttpResponse.json({ data: [] })))

    renderPage()

    expect(await screen.findByText('등록된 소스가 없습니다')).toBeInTheDocument()
  })
})

describe('NewsSourcesPage (추가)', () => {
  it('소스 추가 모달에서 필수·형식 검증 메시지를 표시한다', async () => {
    const user = userEvent.setup()
    renderPage()
    await screen.findByText('우아한형제들 기술블로그')

    await user.click(screen.getByRole('button', { name: /소스 추가/ }))
    await user.click(screen.getByRole('button', { name: '추가' }))

    expect(await screen.findByText('이름을 입력해주세요')).toBeInTheDocument()
    expect(screen.getByText('URL을 입력해주세요')).toBeInTheDocument()

    await user.type(screen.getByPlaceholderText('https://example.com/feed'), 'ftp://example.com/feed')

    expect(await screen.findByText('http(s)로 시작하는 URL을 입력해주세요')).toBeInTheDocument()
  })

  it('생성 성공 시 POST 후 모달을 닫고 성공 토스트를 띄운다', async () => {
    let capturedBody: unknown = null
    server.use(
      http.post(BASE_URL, async ({ request }) => {
        capturedBody = await request.json()
        return HttpResponse.json(
          {
            data: {
              id: 3,
              name: '토스 기술블로그',
              url: 'https://toss.tech/rss.xml',
              isActive: true,
              createdAt: '2026-07-03T10:00:00',
              modifiedDate: '2026-07-03T10:00:00',
            },
          },
          { status: 201 },
        )
      }),
    )

    const user = userEvent.setup()
    renderPage()
    await screen.findByText('우아한형제들 기술블로그')

    await user.click(screen.getByRole('button', { name: /소스 추가/ }))
    await user.type(screen.getByPlaceholderText('소스 이름'), '토스 기술블로그')
    await user.type(screen.getByPlaceholderText('https://example.com/feed'), 'https://toss.tech/rss.xml')
    await user.click(screen.getByRole('button', { name: '추가' }))

    await waitFor(() => {
      expect(capturedBody).toEqual({
        name: '토스 기술블로그',
        url: 'https://toss.tech/rss.xml',
        isActive: true,
      })
    })
    expect(await screen.findByText('소스를 추가했어요.')).toBeInTheDocument()
    // 모달 닫힘 (jsdom에선 leave 애니메이션이 끝나지 않아 DOM 제거 대신 닫힘 상태로 확인)
    await waitFor(() => {
      expect(document.querySelector('.ant-modal.ant-zoom-leave')).toBeInTheDocument()
    })
  })

  it('URL 중복(409)이면 에러 토스트를 띄운다', async () => {
    const user = userEvent.setup()
    renderPage()
    await screen.findByText('우아한형제들 기술블로그')

    await user.click(screen.getByRole('button', { name: /소스 추가/ }))
    await user.type(screen.getByPlaceholderText('소스 이름'), '중복 소스')
    await user.type(
      screen.getByPlaceholderText('https://example.com/feed'),
      'https://duplicate.example.com/feed',
    )
    await user.click(screen.getByRole('button', { name: '추가' }))

    expect(await screen.findByText('이미 등록된 URL입니다.')).toBeInTheDocument()
  })
})

describe('NewsSourcesPage (수정)', () => {
  it('수정 모달에 기존 값을 채우고, 저장 시 PATCH를 호출한다', async () => {
    let capturedId: string | null = null
    let capturedBody: unknown = null
    server.use(
      http.patch(`${BASE_URL}/:id`, async ({ request, params }) => {
        capturedId = String(params.id)
        capturedBody = await request.json()
        return HttpResponse.json({
          data: {
            id: 1,
            name: '우아한형제들 블로그',
            url: 'https://techblog.woowahan.com/feed',
            isActive: true,
            createdAt: '2026-07-01T10:00:00',
            modifiedDate: '2026-07-04T10:00:00',
          },
        })
      }),
    )

    const user = userEvent.setup()
    renderPage()
    await screen.findByText('우아한형제들 기술블로그')

    await user.click(screen.getAllByRole('button', { name: '수정' })[0])

    expect(screen.getByText('소스 수정')).toBeInTheDocument()
    expect(screen.getByDisplayValue('우아한형제들 기술블로그')).toBeInTheDocument()
    expect(screen.getByDisplayValue('https://techblog.woowahan.com/feed')).toBeInTheDocument()

    await user.clear(screen.getByPlaceholderText('소스 이름'))
    await user.type(screen.getByPlaceholderText('소스 이름'), '우아한형제들 블로그')
    await user.click(screen.getByRole('button', { name: '저장' }))

    await waitFor(() => {
      expect(capturedId).toBe('1')
    })
    expect(capturedBody).toEqual({
      name: '우아한형제들 블로그',
      url: 'https://techblog.woowahan.com/feed',
      isActive: true,
    })
    expect(await screen.findByText('소스를 수정했어요.')).toBeInTheDocument()
  })
})

describe('NewsSourcesPage (삭제)', () => {
  it('삭제 확인 모달에서 확인하면 DELETE를 호출한다', async () => {
    let deletedId: string | null = null
    server.use(
      http.delete(`${BASE_URL}/:id`, ({ params }) => {
        deletedId = String(params.id)
        return new HttpResponse(null, { status: 204 })
      }),
    )

    const user = userEvent.setup()
    renderPage()
    await screen.findByText('우아한형제들 기술블로그')

    await user.click(screen.getAllByRole('button', { name: '삭제' })[0])

    const modal = await findConfirmModal()
    expect(within(modal).getByText(/수집 이력은 유지됩니다/)).toBeInTheDocument()

    await user.click(within(modal).getByRole('button', { name: '삭제' }))

    await waitFor(() => {
      expect(deletedId).toBe('1')
    })
    expect(await screen.findByText('소스를 삭제했어요.')).toBeInTheDocument()
  })

  it('삭제 확인 모달에서 취소하면 DELETE를 호출하지 않는다', async () => {
    let deleteCalled = false
    server.use(
      http.delete(`${BASE_URL}/:id`, () => {
        deleteCalled = true
        return new HttpResponse(null, { status: 204 })
      }),
    )

    const user = userEvent.setup()
    renderPage()
    await screen.findByText('우아한형제들 기술블로그')

    await user.click(screen.getAllByRole('button', { name: '삭제' })[0])

    const modal = await findConfirmModal()
    await user.click(within(modal).getByRole('button', { name: '취소' }))

    // 모달 닫힘 시작(leave) 확인 후 DELETE 미호출 검증
    await waitFor(() => {
      expect(document.querySelector('.ant-modal-confirm.ant-zoom-leave')).toBeInTheDocument()
    })
    expect(deleteCalled).toBe(false)
  })
})

describe('NewsSourcesPage (활성 토글)', () => {
  it('스위치 토글 시 PATCH로 isActive만 전송한다', async () => {
    let capturedId: string | null = null
    let capturedBody: unknown = null
    server.use(
      http.patch(`${BASE_URL}/:id`, async ({ request, params }) => {
        capturedId = String(params.id)
        capturedBody = await request.json()
        return HttpResponse.json({
          data: {
            id: 1,
            name: '우아한형제들 기술블로그',
            url: 'https://techblog.woowahan.com/feed',
            isActive: false,
            createdAt: '2026-07-01T10:00:00',
            modifiedDate: '2026-07-04T10:00:00',
          },
        })
      }),
    )

    const user = userEvent.setup()
    renderPage()
    await screen.findByText('우아한형제들 기술블로그')

    await user.click(screen.getAllByRole('switch')[0])

    await waitFor(() => {
      expect(capturedId).toBe('1')
    })
    expect(capturedBody).toEqual({ isActive: false })
  })

  it('토글 실패 시 에러 토스트를 띄우고 스위치 상태를 유지한다', async () => {
    server.use(
      http.patch(`${BASE_URL}/:id`, () =>
        HttpResponse.json(
          { errorMessage: '서버 오류가 발생했습니다.', errorCode: 'INTERNAL_SERVER_ERROR' },
          { status: 500 },
        ),
      ),
    )

    const user = userEvent.setup()
    renderPage()
    await screen.findByText('우아한형제들 기술블로그')

    await user.click(screen.getAllByRole('switch')[0])

    expect(await screen.findByText('서버 오류가 발생했습니다.')).toBeInTheDocument()
    await waitFor(() => {
      expect(screen.getAllByRole('switch')[0]).toBeChecked()
    })
  })
})
