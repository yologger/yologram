import { describe, it, expect } from 'vitest'
import { render, screen } from '@testing-library/react'
import ComingSoon from './ComingSoon'

describe('ComingSoon', () => {
  it('항상 "페이지 준비 중입니다"를 렌더한다', () => {
    render(<ComingSoon />)
    expect(screen.getByText('페이지 준비 중입니다')).toBeInTheDocument()
  })

  it('title이 있으면 상단 헤더에 페이지명을 렌더한다', () => {
    render(<ComingSoon title="유저 관리" />)
    expect(screen.getByText('유저 관리')).toBeInTheDocument()
  })

  it('title이 없으면 헤더 없이 안내만 렌더한다', () => {
    render(<ComingSoon />)
    expect(screen.queryByRole('heading', { level: 3 })).not.toBeInTheDocument()
  })
})
