import { describe, it, expect, beforeAll, afterAll, afterEach } from 'vitest'
import { http, HttpResponse } from 'msw'
import { server } from '../test/server'
import { createPost, getPosts } from './pms'

beforeAll(() => server.listen())
afterEach(() => server.resetHandlers())
afterAll(() => server.close())

describe('createPost', () => {
  it('작성 성공 시 생성된 게시글 id를 반환한다', async () => {
    const result = await createPost('tech', { content: '내용', categoryIds: [1] })
    expect(result).toEqual({ id: 9999 })
  })

  it('카테고리 불일치 시 에러를 던진다', async () => {
    server.use(
      http.post('http://localhost:5001/api/v1/pms/:section/posts', () =>
        HttpResponse.json(
          { errorMessage: '해당 게시판의 카테고리가 아닙니다.', errorCode: 'INVALID_POST_CATEGORY' },
          { status: 400 },
        ),
      ),
    )

    await expect(createPost('tech', { content: '내용', categoryIds: [8] })).rejects.toThrow()
  })
})

describe('getPosts', () => {
  it('목록과 nextCursor를 반환한다', async () => {
    const page = await getPosts('tech', { size: 15 })

    expect(page.data.length).toBeGreaterThan(0)
    expect(page.data[0].author.nickname).toBe('테스터')
    expect(page.nextCursor).toBe('next-cursor')
  })

  it('categoryId로 필터링한다', async () => {
    const page = await getPosts('tech', { categoryId: 2 })

    expect(page.data.every((p) => p.categoryIds.includes(2))).toBe(true)
  })

  it('cursor가 있으면 다음 페이지(빈 결과)를 반환한다', async () => {
    const page = await getPosts('tech', { cursor: 'next-cursor' })

    expect(page.data).toEqual([])
    expect(page.nextCursor).toBeNull()
  })
})
