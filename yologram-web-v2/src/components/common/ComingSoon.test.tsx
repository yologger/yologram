import { describe, it, expect, vi } from 'vitest'
import { render, screen, fireEvent } from '@testing-library/react'
import ComingSoon from './ComingSoon'

const mockPush = vi.fn()
vi.mock('next/navigation', () => ({ useRouter: () => ({ push: mockPush }) }))

describe('ComingSoon', () => {
  it('항상 "페이지 준비 중입니다"를 렌더한다', () => {
    render(<ComingSoon />)
    expect(screen.getByText('페이지 준비 중입니다')).toBeInTheDocument()
  })

  it('title이 있으면 상단 헤더에 페이지명을 렌더한다', () => {
    render(<ComingSoon title="투자" />)
    expect(screen.getByText('투자')).toBeInTheDocument()
  })

  it('"홈으로" 버튼 클릭 시 /tech로 이동한다', () => {
    render(<ComingSoon />)
    fireEvent.click(screen.getByText('홈으로'))
    expect(mockPush).toHaveBeenCalledWith('/tech')
  })
})
