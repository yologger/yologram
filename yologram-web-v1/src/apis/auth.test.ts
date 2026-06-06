import { describe, it, expect, beforeAll, afterAll, afterEach } from 'vitest'
import { server } from '../test/server'
import { join, login, logout, validateToken } from './auth'
import { getDefaultStore } from 'jotai'
import { authAtom } from '../stores/auth'

beforeAll(() => server.listen())
afterEach(() => {
  server.resetHandlers()
  getDefaultStore().set(authAtom, null)
})
afterAll(() => server.close())

describe('join', () => {
  it('회원가입 성공 시 uid를 반환한다', async () => {
    const result = await join({
      email: 'new@yologram.link',
      name: '테스트',
      nickname: 'tester',
      password: 'password123!',
    })

    expect(result).toEqual({ uid: 1 })
  })

  it('이메일 중복 시 에러를 던진다', async () => {
    await expect(
      join({
        email: 'duplicate@yologram.link',
        name: '테스트',
        nickname: 'tester',
        password: 'password123!',
      }),
    ).rejects.toThrow()
  })
})

describe('login', () => {
  it('로그인 성공 시 AuthState를 반환한다', async () => {
    const result = await login('test@yologram.link', 'password123!')

    expect(result).toEqual({
      uid: 1,
      accessToken: 'mock-access-token',
      email: 'test@yologram.link',
      name: '테스터',
      nickname: 'tester',
    })
  })

  it('존재하지 않는 사용자 시 에러를 던진다', async () => {
    await expect(login('notfound@yologram.link', 'password123!')).rejects.toThrow()
  })

  it('비밀번호 불일치 시 에러를 던진다', async () => {
    await expect(login('test@yologram.link', 'wrongpassword')).rejects.toThrow()
  })
})

describe('validateToken', () => {
  it('유효한 토큰이면 사용자 정보를 반환한다', async () => {
    getDefaultStore().set(authAtom, {
      uid: 1,
      accessToken: 'valid-token',
      email: 'test@yologram.link',
      name: '테스터',
      nickname: 'tester',
    })

    const result = await validateToken()

    expect(result).toEqual({
      uid: 1,
      email: 'test@yologram.link',
      name: '테스터',
      nickname: 'tester',
    })
  })

  it('만료된 토큰이면 에러를 던진다', async () => {
    getDefaultStore().set(authAtom, {
      uid: 1,
      accessToken: 'expired-token',
      email: 'test@yologram.link',
      name: '테스터',
      nickname: 'tester',
    })

    await expect(validateToken()).rejects.toThrow()
  })
})

describe('logout', () => {
  it('로그아웃 성공 시 에러 없이 완료된다', async () => {
    getDefaultStore().set(authAtom, {
      uid: 1,
      accessToken: 'valid-token',
      email: 'test@yologram.link',
      name: '테스터',
      nickname: 'tester',
    })

    await expect(logout()).resolves.toBeUndefined()
  })
})
