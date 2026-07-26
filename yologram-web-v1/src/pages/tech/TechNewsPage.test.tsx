import { describe, it, expect, vi, beforeAll, beforeEach, afterEach, afterAll } from 'vitest'
import { screen, waitFor, act } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { http, HttpResponse } from 'msw'
import { renderWithProviders } from '../../test/utils'
import { server } from '../../test/server'
import TechNewsPage from './TechNewsPage'

// jsdom에는 IntersectionObserver가 없어 스텁 처리.
// 무한스크롤 테스트에서 교차를 직접 트리거할 수 있도록 콜백을 수집한다.
let observerCallbacks: IntersectionObserverCallback[] = []

function triggerIntersect() {
  act(() => {
    observerCallbacks.forEach((cb) =>
      cb([{ isIntersecting: true } as IntersectionObserverEntry], {} as IntersectionObserver),
    )
  })
}

beforeAll(() => {
  server.listen()
  vi.stubGlobal(
    'IntersectionObserver',
    class {
      constructor(cb: IntersectionObserverCallback) {
        observerCallbacks.push(cb)
      }
      observe() {}
      unobserve() {}
      disconnect() {}
    },
  )
})
beforeEach(() => {
  observerCallbacks = []
})
afterEach(() => server.resetHandlers())
afterAll(() => {
  server.close()
  vi.unstubAllGlobals()
})

describe('TechNewsPage', () => {
  it('뉴스 목록(출처·카테고리 태그·제목·요약)이 렌더링된다', async () => {
    renderWithProviders(<TechNewsPage />)

    expect(await screen.findByText('첫 번째 뉴스')).toBeInTheDocument()
    expect(screen.getByText('두 번째 뉴스')).toBeInTheDocument()
    expect(screen.getByText('우아한형제들')).toBeInTheDocument()
    // 카테고리 태그 (칩과 중복 표시될 수 있어 개수만 확인)
    expect(screen.getAllByText('Cloud').length).toBeGreaterThan(0)
    // summary 마크다운 렌더: **…** → <strong>, 리스트 → <li>
    const bold = screen.getByText('📌 한 줄 요약')
    expect(bold.tagName).toBe('STRONG')
    expect(screen.getByText('포인트 하나').tagName).toBe('LI')
  })

  it('제목은 원문 링크로 새 탭에서 열린다 (noopener noreferrer)', async () => {
    renderWithProviders(<TechNewsPage />)

    const link = await screen.findByRole('link', { name: '첫 번째 뉴스' })
    expect(link).toHaveAttribute('href', 'https://blog.example.com/a1')
    expect(link).toHaveAttribute('target', '_blank')
    expect(link).toHaveAttribute('rel', 'noopener noreferrer')
  })

  it('카테고리 API에서 받은 칩이 표시되고 선택 시 해당 categoryId로 재조회한다', async () => {
    const user = userEvent.setup()
    renderWithProviders(<TechNewsPage />)

    await screen.findByText('첫 번째 뉴스')

    // '전체' + 카테고리 API(TECH: Frontend/Backend/AI\/ML/기타) 기반 칩
    expect(screen.getByText('전체')).toBeInTheDocument()
    expect((await screen.findAllByText('기타')).length).toBeGreaterThan(0)
    // 고정 상수 칩(Security)은 더 이상 없음 (API 어휘에 없는 라벨)
    expect(screen.queryByText('Security')).not.toBeInTheDocument()

    // 'Frontend'(id=1) 칩 클릭 → Frontend 뉴스만 남음
    await user.click(screen.getAllByText('Frontend')[0])

    expect(await screen.findByText('두 번째 뉴스')).toBeInTheDocument()
    expect(screen.queryByText('첫 번째 뉴스')).not.toBeInTheDocument()
  })

  it('슬래시 포함 라벨(AI/ML) 칩도 categoryId로 정상 필터링한다', async () => {
    const user = userEvent.setup()
    renderWithProviders(<TechNewsPage />)

    await screen.findByText('첫 번째 뉴스')

    // 'AI/ML'(id=3) 칩 클릭
    await user.click((await screen.findAllByText('AI/ML'))[0])

    expect(await screen.findByText('AI 뉴스')).toBeInTheDocument()
    expect(screen.queryByText('첫 번째 뉴스')).not.toBeInTheDocument()
  })

  it('센티널 교차 시 다음 페이지를 이어 붙이고 마지막 페이지면 스크롤을 멈춘다', async () => {
    renderWithProviders(<TechNewsPage />)

    await screen.findByText('첫 번째 뉴스')

    triggerIntersect()

    expect(await screen.findByText('다음 페이지 뉴스')).toBeInTheDocument()
    // 기존 페이지 유지
    expect(screen.getByText('첫 번째 뉴스')).toBeInTheDocument()

    // 마지막 페이지(nextCursor 생략)이므로 재교차해도 추가 조회 없음
    const before = observerCallbacks.length
    triggerIntersect()
    await waitFor(() => {
      expect(observerCallbacks.length).toBe(before)
    })
    expect(screen.getAllByText('다음 페이지 뉴스').length).toBe(1)
  })

  it('뉴스가 없으면 빈 상태 문구를 표시한다', async () => {
    server.use(
      http.get('http://localhost:5001/api/v1/news/tech', () =>
        HttpResponse.json({ data: [] }),
      ),
    )
    renderWithProviders(<TechNewsPage />)

    expect(await screen.findByText('아직 뉴스가 없어요.')).toBeInTheDocument()
  })

  it('조회 실패 시 에러 문구와 다시 시도 버튼을 표시하고, 다시 시도로 재조회한다', async () => {
    server.use(
      http.get('http://localhost:5001/api/v1/news/tech', () =>
        HttpResponse.json(
          { errorMessage: '서버 오류', errorCode: 'INTERNAL_ERROR' },
          { status: 500 },
        ),
      ),
    )
    const user = userEvent.setup()
    renderWithProviders(<TechNewsPage />)

    expect(await screen.findByText(/뉴스를 불러오지 못했어요/)).toBeInTheDocument()

    // 서버 복구 후 다시 시도 → 목록 표시
    server.resetHandlers()
    await user.click(screen.getByRole('button', { name: '다시 시도' }))

    expect(await screen.findByText('첫 번째 뉴스')).toBeInTheDocument()
  })
})
