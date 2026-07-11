import { describe, it, expect } from 'vitest'
import { screen } from '@testing-library/react'
import { renderWithProviders } from './test/utils'
import Router from './Router'

function renderRouter(initialPath: string) {
  return renderWithProviders(<Router />, {
    wrapperOptions: { routerProps: { initialEntries: [initialPath] } },
  })
}

describe('Router', () => {
  it('/ 진입 시 /dashboard로 리다이렉트한다', () => {
    renderRouter('/')
    expect(screen.getByRole('heading', { level: 3, name: '대시보드' })).toBeInTheDocument()
  })

  it.each([
    ['/dashboard', '대시보드'],
    ['/users', '회원 관리'],
    ['/categories', '카테고리 관리'],
    ['/posts', '게시글 관리'],
    ['/feeds', 'RSS 피드 관리'],
  ])('%s 진입 시 "%s" 준비 중 화면을 렌더한다', (path, title) => {
    renderRouter(path)
    expect(screen.getByRole('heading', { level: 3, name: title })).toBeInTheDocument()
    expect(screen.getByText('페이지 준비 중입니다')).toBeInTheDocument()
  })

  it('알 수 없는 경로는 /dashboard로 리다이렉트한다', () => {
    renderRouter('/unknown-path')
    expect(screen.getByRole('heading', { level: 3, name: '대시보드' })).toBeInTheDocument()
  })
})
