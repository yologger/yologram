import { describe, it, expect } from 'vitest'
import { screen } from '@testing-library/react'
import { Routes, Route } from 'react-router'
import { renderWithProviders } from '../../test/utils'
import KeywordSearchPage from './KeywordSearchPage'

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
})
