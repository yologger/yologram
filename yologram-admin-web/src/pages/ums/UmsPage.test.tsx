import { describe, it, expect } from 'vitest'
import { screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { Routes, Route, Navigate } from 'react-router'
import { renderWithProviders } from '../../test/utils'
import UmsPage from './UmsPage'

function renderUmsPage(initialPath = '/ums') {
  return renderWithProviders(
    <Routes>
      <Route path="/ums" element={<UmsPage />}>
        <Route index element={<Navigate to="/ums/users" replace />} />
        <Route path="users" element={<div>users content</div>} />
        <Route path="admin-users" element={<div>admin-users content</div>} />
      </Route>
    </Routes>,
    { wrapperOptions: { routerProps: { initialEntries: [initialPath] } } },
  )
}

describe('UmsPage', () => {
  it('타이틀과 두 서브탭(유저 관리·어드민 관리)을 렌더한다', () => {
    renderUmsPage()

    expect(screen.getByRole('heading', { level: 3, name: '유저 관리' })).toBeInTheDocument()
    expect(screen.getByRole('tab', { name: '유저 관리' })).toBeInTheDocument()
    expect(screen.getByRole('tab', { name: '어드민 관리' })).toBeInTheDocument()
  })

  it('/ums 진입 시 유저 관리 탭이 기본 선택되고 해당 콘텐츠를 렌더한다', () => {
    renderUmsPage('/ums')

    expect(screen.getByRole('tab', { name: '유저 관리' })).toHaveAttribute('aria-selected', 'true')
    expect(screen.getByText('users content')).toBeInTheDocument()
  })

  it('어드민 관리 탭 클릭 시 /ums/admin-users로 이동하고 콘텐츠가 전환된다', async () => {
    const user = userEvent.setup()
    renderUmsPage('/ums')

    await user.click(screen.getByRole('tab', { name: '어드민 관리' }))

    expect(screen.getByRole('tab', { name: '어드민 관리' })).toHaveAttribute('aria-selected', 'true')
    expect(screen.getByText('admin-users content')).toBeInTheDocument()
    expect(screen.queryByText('users content')).not.toBeInTheDocument()
  })

  it('/ums/admin-users 직접 진입 시 어드민 관리 탭이 선택된다', () => {
    renderUmsPage('/ums/admin-users')

    expect(screen.getByRole('tab', { name: '어드민 관리' })).toHaveAttribute('aria-selected', 'true')
    expect(screen.getByText('admin-users content')).toBeInTheDocument()
  })
})
