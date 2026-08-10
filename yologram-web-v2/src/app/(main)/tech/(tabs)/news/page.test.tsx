import { describe, it, expect, vi, beforeAll, beforeEach, afterEach, afterAll } from 'vitest'
import { act, screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { http, HttpResponse } from 'msw'
import { renderWithProviders } from '../../../../test/utils'
import { server } from '../../../../test/server'
import TechNews from './page'

type IOCallback = (entries: Array<{ isIntersecting: boolean }>) => void
let ioCallbacks: IOCallback[] = []

beforeAll(() => {
  server.listen()
  vi.stubGlobal(
    'IntersectionObserver',
    class {
      constructor(callback: IOCallback) {
        ioCallbacks.push(callback)
      }
      observe() {}
      unobserve() {}
      disconnect() {}
    },
  )
})
beforeEach(() => {
  ioCallbacks = []
})
afterEach(() => server.resetHandlers())
afterAll(() => server.close())

describe('TechNews 목록', () => {
  it('API에서 받은 뉴스 카드(출처·카테고리 태그·제목 링크)가 렌더링된다', async () => {
    renderWithProviders(<TechNews />)

    expect(await screen.findByText('Kotlin 코루틴 구조화된 동시성 정리')).toBeInTheDocument()
    expect(screen.getByText('LLM 프롬프트 엔지니어링 가이드')).toBeInTheDocument()

    // 출처
    expect(screen.getByText('카카오 기술블로그')).toBeInTheDocument()
    expect(screen.getByText('네이버 D2')).toBeInTheDocument()

    // 카테고리 태그 (Backend는 필터 칩에도 있을 수 있으므로 존재만 확인)
    expect(screen.getAllByText('Backend').length).toBeGreaterThan(0)
    expect(screen.getByText('Cloud')).toBeInTheDocument()

    // 제목은 원문 링크로 새 탭 열기
    const link = screen.getByRole('link', { name: 'Kotlin 코루틴 구조화된 동시성 정리' })
    expect(link).toHaveAttribute('href', 'https://example.com/news/101')
    expect(link).toHaveAttribute('target', '_blank')
    expect(link).toHaveAttribute('rel', 'noopener noreferrer')
  })

  it('summary 마크다운(볼드·리스트)이 렌더링된다', async () => {
    renderWithProviders(<TechNews />)

    await screen.findByText('Kotlin 코루틴 구조화된 동시성 정리')

    // **핵심 요약** → <strong>
    const bold = screen.getByText('핵심 요약')
    expect(bold.tagName).toBe('STRONG')

    // "- 구조화된 동시성으로 누수 방지" → <li>
    const listItem = screen.getByText('구조화된 동시성으로 누수 방지')
    expect(listItem.closest('li')).not.toBeNull()
  })

  it('카테고리 API 기반 칩(전체 + 카테고리 목록)이 표시된다', async () => {
    renderWithProviders(<TechNews />)

    await screen.findByText('Kotlin 코루틴 구조화된 동시성 정리')

    // cms 카테고리 API mock: Frontend·Backend·AI/ML
    expect(screen.getByText('전체')).toBeInTheDocument()
    expect(await screen.findByText('Frontend')).toBeInTheDocument()
    // Backend·AI/ML은 카드 태그에도 있으므로 존재만 확인
    expect(screen.getAllByText('Backend').length).toBeGreaterThan(0)
    expect(screen.getAllByText('AI/ML').length).toBeGreaterThan(0)
  })

  it('카테고리 칩 선택 시 해당 categoryId로 뉴스를 조회한다', async () => {
    const user = userEvent.setup()
    renderWithProviders(<TechNews />)

    await screen.findByText('Kotlin 코루틴 구조화된 동시성 정리')

    // 'AI/ML'(id=3)은 필터 칩과 카드 배지 모두에 존재 → 첫 번째(칩) 클릭
    const chips = await screen.findAllByText('AI/ML')
    await user.click(chips[0])

    expect(await screen.findByText('LLM 프롬프트 엔지니어링 가이드')).toBeInTheDocument()
    expect(screen.queryByText('Kotlin 코루틴 구조화된 동시성 정리')).not.toBeInTheDocument()
  })

  it('스크롤 센티널 노출 시 다음 페이지를 이어서 불러온다 (무한스크롤)', async () => {
    renderWithProviders(<TechNews />)

    await screen.findByText('Kotlin 코루틴 구조화된 동시성 정리')

    // 센티널 IntersectionObserver 콜백 트리거
    await act(async () => {
      ioCallbacks.at(-1)?.([{ isIntersecting: true }])
    })

    expect(await screen.findByText('커서 이후 뉴스')).toBeInTheDocument()
    // 기존 페이지 뉴스 유지
    expect(screen.getByText('Kotlin 코루틴 구조화된 동시성 정리')).toBeInTheDocument()
  })

  it('뉴스가 없으면 빈 상태 문구를 보여준다', async () => {
    const user = userEvent.setup()
    renderWithProviders(<TechNews />)

    await screen.findByText('Kotlin 코루틴 구조화된 동시성 정리')

    // fixture 1페이지에 Frontend(id=1) 뉴스 없음 → 빈 목록
    await user.click(await screen.findByText('Frontend'))

    expect(await screen.findByText('아직 뉴스가 없어요.')).toBeInTheDocument()
  })

  it('조회 실패 시 에러 문구와 다시 시도 버튼을 보여준다', async () => {
    server.use(
      http.get('http://localhost:5002/api/v2/news/tech', () =>
        HttpResponse.json(
          { errorMessage: '서버 오류', errorCode: 'INTERNAL_SERVER_ERROR' },
          { status: 500 },
        ),
      ),
    )

    renderWithProviders(<TechNews />)

    expect(
      await screen.findByText('뉴스를 불러오지 못했어요. 잠시 후 다시 시도해주세요.'),
    ).toBeInTheDocument()
    expect(screen.getByRole('button', { name: '다시 시도' })).toBeInTheDocument()
  })

  it('다시 시도 클릭 시 재조회하여 목록을 보여준다', async () => {
    const user = userEvent.setup()

    // 첫 요청만 실패, 이후 기본 핸들러로 복구
    server.use(
      http.get(
        'http://localhost:5002/api/v2/news/tech',
        () =>
          HttpResponse.json(
            { errorMessage: '서버 오류', errorCode: 'INTERNAL_SERVER_ERROR' },
            { status: 500 },
          ),
        { once: true },
      ),
    )

    renderWithProviders(<TechNews />)

    await screen.findByText('뉴스를 불러오지 못했어요. 잠시 후 다시 시도해주세요.')
    await user.click(screen.getByRole('button', { name: '다시 시도' }))

    expect(await screen.findByText('Kotlin 코루틴 구조화된 동시성 정리')).toBeInTheDocument()
  })
})
