import { describe, it, expect, beforeAll, afterAll, afterEach, beforeEach } from 'vitest'
import { getDefaultStore } from 'jotai'
import { server } from '../test/server'
import { mockNewsSources } from '../test/handlers'
import { getNewsSources, createNewsSource, updateNewsSource, deleteNewsSource } from './newsSources'
import { authAtom, type AuthState } from '../stores/auth'

const validAuth: AuthState = {
  uid: 1,
  accessToken: 'valid-token',
  email: 'admin@yologram.link',
  name: '관리자',
  role: 'OWNER',
}

beforeAll(() => server.listen())
beforeEach(() => {
  getDefaultStore().set(authAtom, validAuth)
})
afterEach(() => {
  server.resetHandlers()
  getDefaultStore().set(authAtom, null)
})
afterAll(() => server.close())

describe('getNewsSources', () => {
  it('소스 목록을 반환한다', async () => {
    const result = await getNewsSources()

    expect(result).toEqual(mockNewsSources)
  })

  it('토큰이 없으면 에러를 던진다', async () => {
    getDefaultStore().set(authAtom, null)

    await expect(getNewsSources()).rejects.toThrow()
  })
})

describe('createNewsSource', () => {
  it('생성 성공 시 생성된 소스를 반환한다', async () => {
    const result = await createNewsSource({
      name: '토스 기술블로그',
      url: 'https://toss.tech/rss.xml',
    })

    expect(result).toEqual({
      id: 3,
      name: '토스 기술블로그',
      url: 'https://toss.tech/rss.xml',
      isActive: true,
      createdAt: '2026-07-03T10:00:00',
      modifiedDate: '2026-07-03T10:00:00',
    })
  })

  it('isActive를 지정하면 그대로 반영된다', async () => {
    const result = await createNewsSource({
      name: '토스 기술블로그',
      url: 'https://toss.tech/rss.xml',
      isActive: false,
    })

    expect(result.isActive).toBe(false)
  })

  it('URL 중복이면 에러를 던진다', async () => {
    await expect(
      createNewsSource({ name: '중복 소스', url: 'https://duplicate.example.com/feed' }),
    ).rejects.toThrow()
  })

  it('http(s)가 아닌 URL이면 에러를 던진다', async () => {
    await expect(
      createNewsSource({ name: '잘못된 소스', url: 'ftp://example.com/feed' }),
    ).rejects.toThrow()
  })
})

describe('updateNewsSource', () => {
  it('수정 성공 시 수정된 소스를 반환한다', async () => {
    const result = await updateNewsSource(1, { name: '우아한형제들 블로그', isActive: false })

    expect(result.id).toBe(1)
    expect(result.name).toBe('우아한형제들 블로그')
    expect(result.isActive).toBe(false)
  })

  it('존재하지 않는 소스면 에러를 던진다', async () => {
    await expect(updateNewsSource(99999, { name: '없는 소스' })).rejects.toThrow()
  })

  it('URL 중복이면 에러를 던진다', async () => {
    await expect(updateNewsSource(1, { url: 'https://duplicate.example.com/feed' })).rejects.toThrow()
  })
})

describe('deleteNewsSource', () => {
  it('삭제 성공 시 에러 없이 완료된다', async () => {
    await expect(deleteNewsSource(1)).resolves.toBeUndefined()
  })

  it('존재하지 않는 소스면 에러를 던진다', async () => {
    await expect(deleteNewsSource(99999)).rejects.toThrow()
  })
})
