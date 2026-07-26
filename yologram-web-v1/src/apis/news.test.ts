import { describe, it, expect, beforeAll, afterAll, afterEach } from 'vitest'
import { server } from '../test/server'
import { getNews } from './news'

beforeAll(() => server.listen())
afterEach(() => server.resetHandlers())
afterAll(() => server.close())

describe('getNews', () => {
  it('목록과 nextCursor를 반환한다', async () => {
    const page = await getNews({ size: 20 })

    expect(page.data.length).toBeGreaterThan(0)
    expect(page.data[0].sourceName).toBe('우아한형제들')
    expect(page.data[0].categories).toEqual(['Backend', 'Cloud'])
    expect(page.nextCursor).toBe('next-cursor')
  })

  it('categoryId로 필터링한다', async () => {
    // 1 = Frontend (msw 카테고리 마스터 매핑)
    const page = await getNews({ categoryId: 1 })

    expect(page.data.length).toBe(1)
    expect(page.data.every((n) => n.categories.includes('Frontend'))).toBe(true)
  })

  it('categoryId가 null이면 전체를 반환한다', async () => {
    const page = await getNews({ categoryId: null })

    expect(page.data.length).toBe(3)
  })

  it('매칭되는 뉴스가 없는 categoryId면 빈 목록을 반환한다', async () => {
    // 7 = 기타 — 어떤 목 뉴스도 갖지 않는 카테고리
    const page = await getNews({ categoryId: 7 })

    expect(page.data).toEqual([])
  })

  it('cursor가 있으면 다음 페이지를 반환하고 마지막 페이지는 nextCursor가 없다', async () => {
    const page = await getNews({ cursor: 'next-cursor' })

    expect(page.data.map((n) => n.title)).toEqual(['다음 페이지 뉴스'])
    expect(page.nextCursor).toBeUndefined()
  })

  it('잘못된 커서면 400 에러를 던진다', async () => {
    await expect(getNews({ cursor: 'invalid-cursor' })).rejects.toThrow()
  })
})
