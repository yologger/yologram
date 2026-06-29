import { describe, it, expect, beforeAll, afterAll, afterEach } from 'vitest'
import { http, HttpResponse } from 'msw'
import { server } from '../test/server'
import { createPost, updatePost, deletePost, getPosts, getMyPosts } from './pms'
import { getDefaultStore } from 'jotai'
import { authAtom } from '../stores/auth'

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
          { errorMessage: '해당 게시판의 카테고리가 아닙니다.', errorCode: 'INVALID_POST_CATEGORY' },
          { status: 400 },
        ),
      ),
    )

    await expect(createPost('tech', { content: '내용', categoryIds: [8] })).rejects.toThrow()
  })
})

describe('updatePost', () => {
  const store = getDefaultStore()

  beforeAll(() => {
    store.set(authAtom, {
      uid: 1,
      email: 'test@yologram.link',
      name: '테스트',
      nickname: 'tester',
      accessToken: 'mock-access-token',
    })
  })

  afterAll(() => store.set(authAtom, null))

  it('수정 성공 시 정상 resolve된다 (204)', async () => {
    await expect(
      updatePost('tech', 1, { title: '수정 제목', content: '수정 내용', categoryIds: [1] }),
    ).resolves.toBeUndefined()
  })

  it('본인 글이 아니면 에러를 던진다 (403)', async () => {
    server.use(
      http.patch('http://localhost:5002/api/v2/pms/:section/posts/:id', () =>
        HttpResponse.json(
          { errorMessage: '권한이 없습니다.', errorCode: 'POST_FORBIDDEN' },
          { status: 403 },
        ),
      ),
    )

    await expect(
      updatePost('tech', 1, { title: null, content: '내용', categoryIds: [1] }),
    ).rejects.toThrow()
  })

  it('존재하지 않는 글이면 에러를 던진다 (404)', async () => {
    server.use(
      http.patch('http://localhost:5002/api/v2/pms/:section/posts/:id', () =>
        HttpResponse.json(
          { errorMessage: '게시글을 찾을 수 없습니다.', errorCode: 'POST_NOT_FOUND' },
          { status: 404 },
        ),
      ),
    )

    await expect(
      updatePost('tech', 99999, { title: null, content: '내용', categoryIds: [1] }),
    ).rejects.toThrow()
  })
})

describe('deletePost', () => {
  const store = getDefaultStore()

  beforeAll(() => {
    store.set(authAtom, {
      uid: 1,
      email: 'test@yologram.link',
      name: '테스트',
      nickname: 'tester',
      accessToken: 'mock-access-token',
    })
  })

  afterAll(() => store.set(authAtom, null))

  it('삭제 성공 시 정상 resolve된다 (204)', async () => {
    await expect(deletePost('tech', 1)).resolves.toBeUndefined()
  })

  it('본인 글이 아니면 에러를 던진다 (403)', async () => {
    await expect(deletePost('tech', 88888)).rejects.toThrow()
  })

  it('존재하지 않는 글이면 에러를 던진다 (404)', async () => {
    await expect(deletePost('tech', 99999)).rejects.toThrow()
  })

  it('인증 토큰이 없으면 에러를 던진다 (401)', async () => {
    store.set(authAtom, null)

    await expect(deletePost('tech', 1)).rejects.toThrow()

    store.set(authAtom, {
      uid: 1,
      email: 'test@yologram.link',
      name: '테스트',
      nickname: 'tester',
      accessToken: 'mock-access-token',
    })
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

describe('getMyPosts', () => {
  const store = getDefaultStore()

  beforeAll(() => {
    store.set(authAtom, {
      uid: 1,
      email: 'test@yologram.link',
      name: '테스트',
      nickname: 'tester',
      accessToken: 'mock-access-token',
    })
  })

  afterAll(() => store.set(authAtom, null))

  it('section 생략 시 전체 내 글과 nextCursor를 반환한다', async () => {
    const page = await getMyPosts({ size: 20 })

    expect(page.data.length).toBe(3)
    expect(page.data[0].author.nickname).toBe('테스터')
    expect(page.nextCursor).toBe('next-cursor')
  })

  it('section으로 필터링한다', async () => {
    const page = await getMyPosts({ section: 'invest' })

    expect(page.data.every((p) => p.section.toLowerCase() === 'invest')).toBe(true)
    expect(page.data[0].content).toBe('내 투자 글 1')
  })

  it('cursor가 있으면 다음 페이지(빈 결과)를 반환한다', async () => {
    const page = await getMyPosts({ cursor: 'next-cursor' })

    expect(page.data).toEqual([])
    expect(page.nextCursor).toBeNull()
  })

  it('인증 토큰이 없으면 에러를 던진다', async () => {
    store.set(authAtom, null)

    await expect(getMyPosts()).rejects.toThrow()

    store.set(authAtom, {
      uid: 1,
      email: 'test@yologram.link',
      name: '테스트',
      nickname: 'tester',
      accessToken: 'mock-access-token',
    })
  })
})
