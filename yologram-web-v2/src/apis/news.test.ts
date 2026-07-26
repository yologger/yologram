import { describe, it, expect, beforeAll, afterAll, afterEach } from 'vitest'
import { http, HttpResponse } from 'msw'
import { server } from '../test/server'
import { getNews } from './news'

beforeAll(() => server.listen())
afterEach(() => server.resetHandlers())
afterAll(() => server.close())

describe('getNews', () => {
  it('첫 페이지 조회 시 뉴스 목록과 nextCursor를 반환한다', async () => {
    const page = await getNews()

    expect(page.data).toHaveLength(2)
    expect(page.data[0]).toMatchObject({
      id: 101,
      title: 'Kotlin 코루틴 구조화된 동시성 정리',
      sourceName: '카카오 기술블로그',
      categories: ['Backend', 'Cloud'],
    })
    expect(page.nextCursor).toBe('news-next-cursor')
  })

  it('마지막 페이지는 nextCursor 없이 반환된다', async () => {
    const page = await getNews({ cursor: 'news-next-cursor' })

    expect(page.data).toHaveLength(1)
    expect(page.data[0].id).toBe(103)
    expect(page.nextCursor).toBeUndefined()
  })

  it('categoryId로 필터하면 해당 카테고리 뉴스만 반환한다', async () => {
    const page = await getNews({ categoryId: 3 })

    expect(page.data).toHaveLength(1)
    expect(page.data[0].id).toBe(102)
  })

  it('categoryId가 쿼리스트링으로 전달된다', async () => {
    let capturedUrl = ''
    server.use(
      http.get('http://localhost:5002/api/v2/news/tech', ({ request }) => {
        capturedUrl = request.url
        return HttpResponse.json({ data: [] })
      }),
    )

    await getNews({ categoryId: 3 })

    expect(capturedUrl).toContain('categoryId=3')
  })

  it('categoryId가 null이면 categoryId 파라미터를 보내지 않는다', async () => {
    let capturedUrl = ''
    server.use(
      http.get('http://localhost:5002/api/v2/news/tech', ({ request }) => {
        capturedUrl = request.url
        return HttpResponse.json({ data: [] })
      }),
    )

    await getNews({ categoryId: null })

    expect(capturedUrl).not.toContain('categoryId')
  })

  it('size 파라미터가 쿼리스트링으로 전달된다', async () => {
    let capturedUrl = ''
    server.use(
      http.get('http://localhost:5002/api/v2/news/tech', ({ request }) => {
        capturedUrl = request.url
        return HttpResponse.json({ data: [] })
      }),
    )

    await getNews({ size: 20 })

    expect(capturedUrl).toContain('size=20')
  })

  it('잘못된 커서면 에러를 던진다 (400 INVALID_CURSOR)', async () => {
    await expect(getNews({ cursor: 'broken-cursor' })).rejects.toThrow()
  })
})
