import { describe, it, expect, beforeAll, afterEach, afterAll } from 'vitest'
import { screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { http, HttpResponse } from 'msw'
import { Routes, Route } from 'react-router'
import { server } from '../../test/server'
import { renderWithProviders } from '../../test/utils'
import KeywordSearchPage from './KeywordSearchPage'

const NEWS_SEARCH_URL = 'http://localhost:5001/api/v1/search/tech/news'

beforeAll(() => server.listen())
afterEach(() => server.resetHandlers())
afterAll(() => server.close())

function renderPage(path: string, basePath = '/tech') {
  return renderWithProviders(
    <Routes>
      <Route path={`${basePath}/keywords/:keyword`} element={<KeywordSearchPage basePath={basePath} />} />
    </Routes>,
    { wrapperOptions: { routerProps: { initialEntries: [path] } } },
  )
}

describe('KeywordSearchPage', () => {
  it('인코딩된 한글 키워드가 디코딩되어 검색결과 텍스트로 렌더된다', () => {
    renderPage('/tech/keywords/%EC%A0%9C%EB%AF%B8%EB%82%98%EC%9D%B4')
    expect(screen.getByText("'제미나이' 검색결과")).toBeInTheDocument()
  })

  it('영문 키워드도 검색결과 텍스트로 렌더된다', () => {
    renderPage('/tech/keywords/react')
    expect(screen.getByText("'react' 검색결과")).toBeInTheDocument()
  })

  it('SearchBar에 현재 키워드가 initialValue로 표시된다', () => {
    renderPage('/tech/keywords/%EC%A0%9C%EB%AF%B8%EB%82%98%EC%9D%B4')
    expect(screen.getByDisplayValue('제미나이')).toBeInTheDocument()
  })

  it('다른 섹션(basePath)에서도 동일하게 동작한다', () => {
    renderPage('/invest/keywords/%EC%82%BC%EC%84%B1%EC%A0%84%EC%9E%90', '/invest')
    expect(screen.getByText("'삼성전자' 검색결과")).toBeInTheDocument()
  })

  it('커뮤니티·뉴스 탭을 렌더한다', () => {
    renderPage('/tech/keywords/%EC%A0%9C%EB%AF%B8%EB%82%98%EC%9D%B4')

    expect(screen.getByRole('tab', { name: '커뮤니티' })).toBeInTheDocument()
    expect(screen.getByRole('tab', { name: '뉴스' })).toBeInTheDocument()
  })

  it('뉴스 탭은 뉴스 검색 결과를 보여준다', async () => {
    server.use(
      http.get(NEWS_SEARCH_URL, () =>
        HttpResponse.json({
          data: [
            {
              id: 900,
              title: '제미나이 3 공개',
              summary: '요약',
              link: 'https://news.test/900',
              sourceName: 'GeekNews',
              categories: ['AI'],
              publishedAt: '2026-08-10T16:23:47',
            },
          ],
          page: 0,
          size: 10,
          totalPages: 1,
          totalCount: 1,
          first: true,
          last: true,
        })
      )
    )
    const user = userEvent.setup()
    renderPage('/tech/keywords/%EC%A0%9C%EB%AF%B8%EB%82%98%EC%9D%B4')

    await user.click(screen.getByRole('tab', { name: '뉴스' }))

    // 커뮤니티 탭과 별개로 자기 검색·페이징을 갖는다
    expect(await screen.findByText('제미나이 3 공개')).toBeInTheDocument()
    expect(await screen.findByText('총 1건')).toBeInTheDocument()
  })
})
