import { describe, it, expect, vi } from 'vitest'
import { render, screen, fireEvent } from '@testing-library/react'
import ComingSoon from './ComingSoon'

const mockNavigate = vi.fn()
vi.mock('react-router', async (importOriginal) => {
  const actual = await importOriginal<typeof import('react-router')>()
  return { ...actual, useNavigate: () => mockNavigate }
})

describe('ComingSoon', () => {
  it('항상 "페이지 준비 중입니다"를 렌더한다', () => {
    render(<ComingSoon />)
    expect(screen.getByText('페이지 준비 중입니다')).toBeInTheDocument()
  })

  it('"홈으로" 버튼 클릭 시 /tech로 이동한다', () => {
    render(<ComingSoon />)
    fireEvent.click(screen.getByText('홈으로'))
    expect(mockNavigate).toHaveBeenCalledWith('/tech')
  })
})
