import { describe, it, expect } from 'vitest'
import { formatRelativeTime } from './date'

describe('formatRelativeTime', () => {
  const now = new Date('2026-06-19T12:00:00')

  it('1분 미만은 방금 전', () => {
    expect(formatRelativeTime('2026-06-19T11:59:30', now)).toBe('방금 전')
  })

  it('분 단위', () => {
    expect(formatRelativeTime('2026-06-19T11:30:00', now)).toBe('30분 전')
  })

  it('시간 단위', () => {
    expect(formatRelativeTime('2026-06-19T09:00:00', now)).toBe('3시간 전')
  })

  it('일 단위', () => {
    expect(formatRelativeTime('2026-06-17T12:00:00', now)).toBe('2일 전')
  })

  it('일주일 이상은 날짜(YYYY.MM.DD)', () => {
    expect(formatRelativeTime('2026-06-01T12:00:00', now)).toBe('2026.06.01')
  })

  it('미래 시각은 방금 전', () => {
    expect(formatRelativeTime('2026-06-19T12:30:00', now)).toBe('방금 전')
  })
})
