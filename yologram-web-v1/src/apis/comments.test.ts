import { describe, it, expect, beforeAll, afterAll, afterEach } from 'vitest'
import { http, HttpResponse } from 'msw'
import { getDefaultStore } from 'jotai'
import { server } from '../test/server'
import { authAtom } from '../stores/auth'
import { createComment, getComments, deleteComment } from './comments'

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
    const result = await createComment('tech', 1, '좋은 글이네요')
    expect(result).toEqual({ id: 5001 })
  })

  it('내용 누락 시 에러를 던진다', async () => {
    login()
    await expect(createComment('tech', 1, '   ')).rejects.toThrow()
  })

  it('존재하지 않는 글이면 404 에러를 던진다', async () => {
    login()
    await expect(createComment('tech', 99999, '내용')).rejects.toThrow()
  })

  it('미인증 상태면 401 에러를 던진다', async () => {
    await expect(createComment('tech', 1, '내용')).rejects.toThrow()
  })

  it('서버 오류 시 에러를 던진다', async () => {
    login()
    server.use(
      http.post('http://localhost:5001/api/v1/comments/:section/posts/:postId', () =>
        HttpResponse.json(
          { errorMessage: '서버 오류', errorCode: 'INTERNAL_SERVER_ERROR' },
          { status: 500 },
        ),
      ),
    )
    await expect(createComment('tech', 1, '내용')).rejects.toThrow()
  })
})

describe('getComments', () => {
  it('댓글 목록과 nextCursor를 반환한다 (최신순 기본)', async () => {
    const page = await getComments('tech', 1)
    expect(page.data).toHaveLength(2)
    expect(page.data[0].content).toBe('최신 댓글')
    expect(page.nextCursor).toBe('next-cursor')
  })

  it('오래된순 정렬 시 오래된 댓글이 먼저 온다', async () => {
    const page = await getComments('tech', 1, { sort: 'oldest' })
    expect(page.data[0].content).toBe('오래된 댓글')
  })

  it('커서를 전달하면 다음(마지막) 페이지를 반환한다', async () => {
    const page = await getComments('tech', 1, { cursor: 'next-cursor' })
    expect(page.data).toHaveLength(0)
    expect(page.nextCursor).toBeNull()
  })

  it('서버 오류 시 에러를 던진다', async () => {
    server.use(
      http.get('http://localhost:5001/api/v1/comments/:section/posts/:postId', () =>
        HttpResponse.json(
          { errorMessage: '서버 오류', errorCode: 'INTERNAL_SERVER_ERROR' },
          { status: 500 },
        ),
      ),
    )
    await expect(getComments('tech', 1)).rejects.toThrow()
  })
})

describe('deleteComment', () => {
  it('본인 댓글 삭제 성공 시 정상 반환한다', async () => {
    login()
    await expect(deleteComment('tech', 101)).resolves.toBeUndefined()
  })

  it('타인 댓글이면 403 에러를 던진다', async () => {
    login()
    await expect(deleteComment('tech', 102)).rejects.toThrow()
  })

  it('존재하지 않는 댓글이면 404 에러를 던진다', async () => {
    login()
    await expect(deleteComment('tech', 99999)).rejects.toThrow()
  })

  it('미인증 상태면 401 에러를 던진다', async () => {
    await expect(deleteComment('tech', 101)).rejects.toThrow()
  })
})
