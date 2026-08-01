import { describe, it, expect, beforeAll, beforeEach, afterEach, afterAll } from 'vitest'
import { screen, waitFor, within } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { getDefaultStore } from 'jotai'
import { http, HttpResponse } from 'msw'
import { server } from '../../test/server'
import { mockAdminUsers, buildAdminUsersPage } from '../../test/handlers'
import { renderWithProviders } from '../../test/utils'
import { authAtom, type AuthState } from '../../stores/auth'
import AdminUsersPage from './AdminUsersPage'

const adminAuth: AuthState = {
  uid: 1,
  accessToken: 'valid-token',
  email: 'admin@yologram.link',
  name: '관리자',
}

const BASE_URL = 'http://localhost:5001/api/v1/ums/admin/admin-users'

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
  return renderWithProviders(<AdminUsersPage />)
}

/** modal.confirm(삭제 확인)은 body portal에 렌더된다 */
async function findConfirmModal() {
  await waitFor(() => {
    expect(document.querySelector('.ant-modal-confirm')).toBeInTheDocument()
  })
  return document.querySelector('.ant-modal-confirm') as HTMLElement
}

describe('AdminUsersPage (목록)', () => {
  it('타이틀·어드민 추가 버튼과 목록 테이블을 렌더한다', async () => {
    renderPage()

    expect(screen.getByRole('heading', { level: 3, name: '어드민 관리' })).toBeInTheDocument()
    expect(screen.getByRole('button', { name: /어드민 추가/ })).toBeInTheDocument()

    expect(await screen.findByText('admin@yologram.link')).toBeInTheDocument()
    expect(screen.getByText('second@yologram.link')).toBeInTheDocument()
    expect(screen.getAllByText('활성').length).toBeGreaterThan(0)
    expect(screen.getByText('비활성')).toBeInTheDocument()
    expect(screen.getByText('2026-06-01')).toBeInTheDocument()
  })

  it('본인 행에는 나 태그를 표시하고 삭제 버튼을 비활성화한다', async () => {
    renderPage()
    await screen.findByText('admin@yologram.link')

    expect(screen.getByText('나')).toBeInTheDocument()

    const deleteButtons = screen.getAllByRole('button', { name: '삭제' })
    expect(deleteButtons).toHaveLength(10)
    // uid 1(본인) 행은 비활성, 나머지 행은 활성
    expect(deleteButtons[0]).toBeDisabled()
    expect(deleteButtons[1]).toBeEnabled()
  })

  it('빈 목록이면 안내 문구를 렌더한다', async () => {
    server.use(http.get(BASE_URL, () => HttpResponse.json(buildAdminUsersPage([], 0, 10))))

    renderPage()

    expect(await screen.findByText('등록된 어드민이 없습니다')).toBeInTheDocument()
  })
})

describe('AdminUsersPage (페이지네이션)', () => {
  it('총 12명이면 2페이지 컨트롤을 렌더하고 첫 페이지 10명만 보여준다', async () => {
    renderPage()
    await screen.findByText('admin@yologram.link')

    expect(screen.getByTitle('1')).toBeInTheDocument()
    expect(screen.getByTitle('2')).toBeInTheDocument()
    expect(screen.getByText('admin10@yologram.link')).toBeInTheDocument()
    expect(screen.queryByText('admin11@yologram.link')).not.toBeInTheDocument()
  })

  it('2페이지 이동 시 두 번째 목록을 렌더한다', async () => {
    const user = userEvent.setup()
    renderPage()
    await screen.findByText('admin@yologram.link')

    await user.click(screen.getByTitle('2'))

    expect(await screen.findByText('admin11@yologram.link')).toBeInTheDocument()
    expect(screen.getByText('admin12@yologram.link')).toBeInTheDocument()
    expect(screen.queryByText('admin@yologram.link')).not.toBeInTheDocument()
  })

  it('페이지의 마지막 항목을 삭제하면 이전 페이지로 보정한다', async () => {
    // 11명 데이터셋 — 2페이지에 1명만 남는 시나리오
    const elevenUsers = mockAdminUsers.slice(0, 11)
    server.use(
      http.get(BASE_URL, ({ request }) => {
        const url = new URL(request.url)
        const page = Number(url.searchParams.get('page') ?? 0)
        return HttpResponse.json(buildAdminUsersPage(elevenUsers, page, 10))
      }),
      http.delete(`${BASE_URL}/:id`, () => new HttpResponse(null, { status: 204 })),
    )

    const user = userEvent.setup()
    renderPage()
    await screen.findByText('admin@yologram.link')

    await user.click(screen.getByTitle('2'))
    await screen.findByText('admin11@yologram.link')

    await user.click(screen.getByRole('button', { name: '삭제' }))
    const modal = await findConfirmModal()
    await user.click(within(modal).getByRole('button', { name: '삭제' }))

    expect(await screen.findByText('어드민을 삭제했어요.')).toBeInTheDocument()
    // 빈 2페이지 대신 1페이지로 보정되어 첫 페이지 목록이 보인다
    expect(await screen.findByText('admin@yologram.link')).toBeInTheDocument()
    await waitFor(() => {
      expect(screen.queryByText('admin11@yologram.link')).not.toBeInTheDocument()
    })
  })
})

describe('AdminUsersPage (추가)', () => {
  it('어드민 추가 모달에서 필수·형식 검증 메시지를 표시한다', async () => {
    const user = userEvent.setup()
    renderPage()
    await screen.findByText('admin@yologram.link')

    await user.click(screen.getByRole('button', { name: /어드민 추가/ }))
    await user.click(screen.getByRole('button', { name: '추가' }))

    expect(await screen.findByText('이메일을 입력해주세요')).toBeInTheDocument()
    expect(screen.getByText('이름을 입력해주세요')).toBeInTheDocument()
    expect(screen.getByText('비밀번호를 입력해주세요')).toBeInTheDocument()

    await user.type(screen.getByPlaceholderText('admin@yologram.link'), 'not-an-email')
    expect(await screen.findByText('올바른 이메일 형식이 아닙니다')).toBeInTheDocument()

    await user.type(screen.getByPlaceholderText('이름'), '가')
    expect(await screen.findByText('이름은 2~20자로 입력해주세요')).toBeInTheDocument()

    await user.type(screen.getByPlaceholderText('비밀번호'), 'short')
    expect(await screen.findByText('비밀번호는 8~20자로 입력해주세요')).toBeInTheDocument()
  })

  it('생성 성공 시 POST 후 모달을 닫고 성공 토스트를 띄운다', async () => {
    let capturedBody: unknown = null
    server.use(
      http.post(BASE_URL, async ({ request }) => {
        capturedBody = await request.json()
        return HttpResponse.json({ data: { uid: 3 } }, { status: 201 })
      }),
    )

    const user = userEvent.setup()
    renderPage()
    await screen.findByText('admin@yologram.link')

    await user.click(screen.getByRole('button', { name: /어드민 추가/ }))
    await user.type(screen.getByPlaceholderText('admin@yologram.link'), 'new-admin@yologram.link')
    await user.type(screen.getByPlaceholderText('이름'), '신규관리자')
    await user.type(screen.getByPlaceholderText('비밀번호'), 'password123!')
    await user.click(screen.getByRole('button', { name: '추가' }))

    await waitFor(() => {
      expect(capturedBody).toEqual({
        email: 'new-admin@yologram.link',
        name: '신규관리자',
        password: 'password123!',
      })
    })
    expect(await screen.findByText('어드민을 추가했어요.')).toBeInTheDocument()
    // 모달 닫힘 (jsdom에선 leave 애니메이션이 끝나지 않아 닫힘 상태로 확인)
    await waitFor(() => {
      expect(document.querySelector('.ant-modal.ant-zoom-leave')).toBeInTheDocument()
    })
  })

  it('이메일 중복(409)이면 에러 토스트를 띄운다', async () => {
    const user = userEvent.setup()
    renderPage()
    await screen.findByText('admin@yologram.link')

    await user.click(screen.getByRole('button', { name: /어드민 추가/ }))
    await user.type(screen.getByPlaceholderText('admin@yologram.link'), 'duplicate@yologram.link')
    await user.type(screen.getByPlaceholderText('이름'), '중복관리자')
    await user.type(screen.getByPlaceholderText('비밀번호'), 'password123!')
    await user.click(screen.getByRole('button', { name: '추가' }))

    expect(await screen.findByText('이미 등록된 이메일입니다.')).toBeInTheDocument()
  })
})

describe('AdminUsersPage (삭제)', () => {
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
    await screen.findByText('admin@yologram.link')

    // 활성화된 삭제 버튼은 uid 2(부관리자) 행
    await user.click(screen.getAllByRole('button', { name: '삭제' })[1])

    const modal = await findConfirmModal()
    expect(within(modal).getByText(/'부관리자' 어드민을 삭제할까요/)).toBeInTheDocument()

    await user.click(within(modal).getByRole('button', { name: '삭제' }))

    await waitFor(() => {
      expect(deletedId).toBe('2')
    })
    expect(await screen.findByText('어드민을 삭제했어요.')).toBeInTheDocument()
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
    await screen.findByText('admin@yologram.link')

    await user.click(screen.getAllByRole('button', { name: '삭제' })[1])

    const modal = await findConfirmModal()
    await user.click(within(modal).getByRole('button', { name: '취소' }))

    await waitFor(() => {
      expect(document.querySelector('.ant-modal-confirm.ant-zoom-leave')).toBeInTheDocument()
    })
    expect(deleteCalled).toBe(false)
  })

  it('삭제 실패(400) 시 서버 에러 메시지를 토스트로 띄운다', async () => {
    server.use(
      http.delete(`${BASE_URL}/:id`, () =>
        HttpResponse.json(
          { errorMessage: '자기 자신은 삭제할 수 없습니다.', errorCode: 'ADMIN_USER_SELF_DELETE' },
          { status: 400 },
        ),
      ),
    )

    const user = userEvent.setup()
    renderPage()
    await screen.findByText('admin@yologram.link')

    await user.click(screen.getAllByRole('button', { name: '삭제' })[1])

    const modal = await findConfirmModal()
    await user.click(within(modal).getByRole('button', { name: '삭제' }))

    expect(await screen.findByText('자기 자신은 삭제할 수 없습니다.')).toBeInTheDocument()
  })
})
