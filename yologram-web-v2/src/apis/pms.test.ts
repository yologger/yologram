import { describe, it, expect, beforeAll, afterAll, afterEach } from 'vitest'
import { http, HttpResponse } from 'msw'
import { server } from '../test/server'
import { createPost } from './pms'

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
      http.post('http://localhost:5002/api/v2/pms/:section/posts', () =>
        HttpResponse.json(
          { errorMessage: '해당 게시판의 카테고리가 아닙니다.', errorCode: 'INVALID_CATEGORY' },
          { status: 400 },
        ),
      ),
    )

    await expect(createPost('tech', { content: '내용', categoryIds: [8] })).rejects.toThrow()
  })
})
