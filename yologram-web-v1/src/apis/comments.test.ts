import { describe, it, expect, beforeAll, afterAll, afterEach } from 'vitest'
import { http, HttpResponse } from 'msw'
import { getDefaultStore } from 'jotai'
import { server } from '../test/server'
import { authAtom } from '../stores/auth'
import { createComment } from './comments'

const login = () =>
  getDefaultStore().set(authAtom, {
    uid: 1,
    accessToken: 'valid-token',
    email: 'test@yologram.link',
    name: '테스터',
    nickname: 'tester',
  })

beforeAll(() => server.listen())
afterEach(() => {
  server.resetHandlers()
  getDefaultStore().set(authAtom, null)
})
afterAll(() => server.close())

describe('createComment', () => {
  it('작성 성공 시 생성된 댓글 id를 반환한다', async () => {
    login()
    const result = await createComment(1, '좋은 글이네요')
    expect(result).toEqual({ id: 5001 })
  })

  it('내용 누락 시 에러를 던진다', async () => {
    login()
    await expect(createComment(1, '   ')).rejects.toThrow()
  })

  it('존재하지 않는 글이면 404 에러를 던진다', async () => {
    login()
    await expect(createComment(99999, '내용')).rejects.toThrow()
  })

  it('미인증 상태면 401 에러를 던진다', async () => {
    await expect(createComment(1, '내용')).rejects.toThrow()
  })

  it('서버 오류 시 에러를 던진다', async () => {
    login()
    server.use(
      http.post('http://localhost:5001/api/v1/comments/posts/:postId', () =>
        HttpResponse.json(
          { errorMessage: '서버 오류', errorCode: 'INTERNAL_SERVER_ERROR' },
          { status: 500 },
        ),
      ),
    )
    await expect(createComment(1, '내용')).rejects.toThrow()
  })
})
