import { describe, it, expect, beforeAll, afterAll, afterEach } from 'vitest'
import { http, HttpResponse } from 'msw'
import { getDefaultStore } from 'jotai'
import { server } from '../test/server'
import { authAtom } from '../stores/auth'
import { createPost, getPosts, likePost, unlikePost } from './pms'

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

  it('게시글에 중첩된 metrics(commentCount/likeCount/likedByMe)를 반환한다', async () => {
    const page = await getPosts('tech', { size: 15 })

    expect(page.data[0].metrics).toEqual({ commentCount: 1, likeCount: 3, likedByMe: false })
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

describe('likePost', () => {
  it('로그인 상태에서 좋아요 등록에 성공한다 (200)', async () => {
    login()
    await expect(likePost('tech', 1)).resolves.toBeUndefined()
  })

  it('존재하지 않는 글이면 404 에러를 던진다', async () => {
    login()
    await expect(likePost('tech', 99999)).rejects.toThrow()
  })

  it('미인증 상태면 401 에러를 던진다', async () => {
    await expect(likePost('tech', 1)).rejects.toThrow()
  })
})

describe('unlikePost', () => {
  it('로그인 상태에서 좋아요 취소에 성공한다 (200, 안 눌렀어도 no-op)', async () => {
    login()
    await expect(unlikePost('tech', 1)).resolves.toBeUndefined()
  })

  it('존재하지 않는 글이면 404 에러를 던진다', async () => {
    login()
    await expect(unlikePost('tech', 99999)).rejects.toThrow()
  })

  it('미인증 상태면 401 에러를 던진다', async () => {
    await expect(unlikePost('tech', 1)).rejects.toThrow()
  })
})
