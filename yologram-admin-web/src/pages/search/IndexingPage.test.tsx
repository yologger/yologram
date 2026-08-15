import { describe, it, expect, beforeAll, beforeEach, afterEach, afterAll, vi } from 'vitest'
import { screen, waitFor, within } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { getDefaultStore } from 'jotai'
import { http, HttpResponse } from 'msw'
import { server } from '../../test/server'
import { renderWithProviders } from '../../test/utils'
import { authAtom, type AuthState } from '../../stores/auth'
import IndexingPage from './IndexingPage'

const adminAuth: AuthState = {
  uid: 1,
  accessToken: 'valid-token',
  email: 'admin@yologram.link',
  name: '관리자',
  role: 'OWNER',
}

const BASE_URL = 'http://localhost:5001/api/v1/search/admin/tech/posts/indexing'
const NEWS_BASE_URL = 'http://localhost:5001/api/v1/search/admin/tech/news/indexing'

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
  return renderWithProviders(
    <IndexingPage section="tech" sectionLabel="기술" target="posts" targetLabel="게시글" />
  )
}

function renderNewsPage() {
  return renderWithProviders(
    <IndexingPage section="tech" sectionLabel="기술" target="news" targetLabel="뉴스" />
  )
}

/** modal.confirm은 body portal에 렌더된다 */
async function findConfirmModal() {
  await waitFor(() => {
    expect(document.querySelector('.ant-modal-confirm')).toBeInTheDocument()
  })
  return document.querySelector('.ant-modal-confirm') as HTMLElement
}

describe('IndexingPage (렌더)', () => {
  it('세 가지 인덱싱 블록을 렌더한다', async () => {
    renderPage()

    expect(await screen.findByText('기술 게시글 인덱싱')).toBeInTheDocument()
    expect(screen.getByText('전체 인덱싱')).toBeInTheDocument()
    expect(screen.getByText('범위 인덱싱')).toBeInTheDocument()
    expect(screen.getByText('단건 인덱싱')).toBeInTheDocument()
  })
})

describe('IndexingPage (전체 인덱싱)', () => {
  it('확인 후 실행하면 전체 인덱싱을 발행한다', async () => {
    const requested = vi.fn()
    server.use(
      http.put(BASE_URL, () => {
        requested()
        return new HttpResponse(null, { status: 202 })
      })
    )
    const user = userEvent.setup()
    renderPage()

    await user.click(await screen.findByRole('button', { name: /전체 인덱싱 실행/ }))
    const modal = await findConfirmModal()
    await user.click(await within(modal).findByRole('button', { name: '실행' }))

    await waitFor(() => expect(requested).toHaveBeenCalledTimes(1))
    expect(await screen.findByText(/인덱싱 작업을 발행했습니다 \(전체\)/)).toBeInTheDocument()
  })

  it('확인 모달에서 취소하면 발행하지 않는다', async () => {
    const requested = vi.fn()
    server.use(
      http.put(BASE_URL, () => {
        requested()
        return new HttpResponse(null, { status: 202 })
      })
    )
    const user = userEvent.setup()
    renderPage()

    await user.click(await screen.findByRole('button', { name: /전체 인덱싱 실행/ }))
    const modal = await findConfirmModal()
    await user.click(await within(modal).findByRole('button', { name: '취소' }))

    // 모달 닫힘 시작(leave) 확인 후 미호출 검증 (NewsSourcesPage 테스트와 같은 방식 —
    // antd 모달은 취소 직후 DOM에서 즉시 제거되지 않는다)
    await waitFor(() => {
      expect(document.querySelector('.ant-modal-confirm.ant-zoom-leave')).toBeInTheDocument()
    })
    expect(requested).not.toHaveBeenCalled()
  })
})

describe('IndexingPage (범위 인덱싱)', () => {
  it('입력한 범위로 발행한다', async () => {
    let calledPath = ''
    server.use(
      http.put(`${BASE_URL}/:from/:to`, ({ params }) => {
        calledPath = `${params.from}/${params.to}`
        return new HttpResponse(null, { status: 202 })
      })
    )
    const user = userEvent.setup()
    renderPage()

    await user.type(await screen.findByPlaceholderText('시작 id'), '1')
    await user.type(screen.getByPlaceholderText('끝 id'), '45')
    await user.click(screen.getByRole('button', { name: /범위 인덱싱 실행/ }))

    await waitFor(() => expect(calledPath).toBe('1/45'))
    expect(await screen.findByText(/인덱싱 작업을 발행했습니다 \(1 ~ 45\)/)).toBeInTheDocument()
  })

  it('값을 비우고 실행하면 검증 메시지를 띄우고 요청하지 않는다', async () => {
    const requested = vi.fn()
    server.use(
      http.put(`${BASE_URL}/:from/:to`, () => {
        requested()
        return new HttpResponse(null, { status: 202 })
      })
    )
    const user = userEvent.setup()
    renderPage()

    await user.click(await screen.findByRole('button', { name: /범위 인덱싱 실행/ }))

    expect(await screen.findByText('시작 id를 입력하세요.')).toBeInTheDocument()
    expect(screen.getByText('끝 id를 입력하세요.')).toBeInTheDocument()
    expect(requested).not.toHaveBeenCalled()
  })

  it('from이 to보다 크면 서버 오류 메시지를 보여준다', async () => {
    // 범위 검증은 서버(400 INVALID_INDEX_RANGE)가 판정한다 — 프론트는 그 메시지를 그대로 노출
    server.use(
      http.put(`${BASE_URL}/:from/:to`, () =>
        HttpResponse.json(
          { errorMessage: '인덱싱 범위가 유효하지 않습니다.', errorCode: 'INVALID_INDEX_RANGE' },
          { status: 400 }
        )
      )
    )
    const user = userEvent.setup()
    renderPage()

    await user.type(await screen.findByPlaceholderText('시작 id'), '30')
    await user.type(screen.getByPlaceholderText('끝 id'), '10')
    await user.click(screen.getByRole('button', { name: /범위 인덱싱 실행/ }))

    expect(await screen.findByText('인덱싱 범위가 유효하지 않습니다.')).toBeInTheDocument()
  })
})

describe('IndexingPage (단건 인덱싱)', () => {
  it('입력한 id로 발행한다', async () => {
    let calledId = ''
    server.use(
      http.put(`${BASE_URL}/:id`, ({ params }) => {
        calledId = String(params.id)
        return new HttpResponse(null, { status: 202 })
      })
    )
    const user = userEvent.setup()
    renderPage()

    await user.type(await screen.findByPlaceholderText('게시글 id'), '1200')
    await user.click(screen.getByRole('button', { name: /단건 인덱싱 실행/ }))

    await waitFor(() => expect(calledId).toBe('1200'))
    expect(await screen.findByText(/인덱싱 작업을 발행했습니다 \(id 1200\)/)).toBeInTheDocument()
  })

  it('값을 비우고 실행하면 검증 메시지를 띄운다', async () => {
    const user = userEvent.setup()
    renderPage()

    await user.click(await screen.findByRole('button', { name: /단건 인덱싱 실행/ }))

    expect(await screen.findByText('게시글 id를 입력하세요.')).toBeInTheDocument()
  })
})

// 대상(target)만 바뀌면 경로·라벨이 따라가는지 — 게시글과 같은 화면을 공유하므로 조작 자체는 위에서 검증됐다
describe('IndexingPage (뉴스 대상)', () => {
  it('타이틀과 입력 placeholder가 뉴스로 바뀐다', async () => {
    renderNewsPage()

    expect(await screen.findByText('기술 뉴스 인덱싱')).toBeInTheDocument()
    expect(await screen.findByPlaceholderText('뉴스 id')).toBeInTheDocument()
  })

  it('단건 인덱싱이 뉴스 경로로 나간다', async () => {
    const user = userEvent.setup()
    let calledId: string | undefined
    server.use(
      http.put(`${NEWS_BASE_URL}/:id`, ({ params }) => {
        calledId = params.id as string
        return new HttpResponse(null, { status: 202 })
      })
    )
    renderNewsPage()

    await user.type(await screen.findByPlaceholderText('뉴스 id'), '900')
    await user.click(screen.getByRole('button', { name: '단건 인덱싱 실행' }))

    await waitFor(() => expect(calledId).toBe('900'))
  })

  it('전체 인덱싱이 뉴스 경로로 나간다', async () => {
    const user = userEvent.setup()
    let called = false
    server.use(
      http.put(NEWS_BASE_URL, () => {
        called = true
        return new HttpResponse(null, { status: 202 })
      })
    )
    renderNewsPage()

    await user.click(await screen.findByRole('button', { name: /전체 인덱싱 실행/ }))
    await findConfirmModal()
    await user.click(within(document.querySelector('.ant-modal-confirm') as HTMLElement).getByRole('button', { name: '실행' }))

    await waitFor(() => expect(called).toBe(true))
  })
})
