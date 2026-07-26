import { describe, it, expect } from 'vitest'
import { renderHook, act } from '@testing-library/react'
import useIsMobile from './useIsMobile'

function setWidth(width: number) {
  Object.defineProperty(window, 'innerWidth', { writable: true, configurable: true, value: width })
}

describe('useIsMobile', () => {
  it('768px 미만이면 true를 반환한다', () => {
    setWidth(767)
    const { result } = renderHook(() => useIsMobile())
    expect(result.current).toBe(true)
  })

  it('768px 이상이면 false를 반환한다', () => {
    setWidth(768)
    const { result } = renderHook(() => useIsMobile())
    expect(result.current).toBe(false)
  })

  it('resize 시 값이 갱신된다', () => {
    setWidth(1024)
    const { result } = renderHook(() => useIsMobile())
    expect(result.current).toBe(false)

    act(() => {
      setWidth(375)
      window.dispatchEvent(new Event('resize'))
    })
    expect(result.current).toBe(true)
  })
})
