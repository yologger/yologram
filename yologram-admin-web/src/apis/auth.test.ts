import { describe, it, expect, beforeAll, afterAll, afterEach } from 'vitest'
import { getDefaultStore } from 'jotai'
import { server } from '../test/server'
import { login, validateToken, logout, createAdminUser } from './auth'
import { authAtom, type AuthState } from '../stores/auth'

const validAuth: AuthState = {
  uid: 1,
  accessToken: 'valid-token',
  email: 'admin@yologram.link',
  name: '관리자',
}

beforeAll(() => server.listen())
afterEach(() => {
  server.resetHandlers()
  getDefaultStore().set(authAtom, null)
})
afterAll(() => server.close())

describe('login', () => {
  it('로그인 성공 시 AuthState를 반환한다', async () => {
    const result = await login('admin@yologram.link', 'password123!')

    expect(result).toEqual({
      uid: 1,
      accessToken: 'mock-access-token',
      email: 'admin@yologram.link',
      name: '관리자',
    })
  })

  it('존재하지 않는 어드민이면 에러를 던진다', async () => {
    await expect(login('notfound@yologram.link', 'password123!')).rejects.toThrow()
  })

  it('비밀번호 불일치 시 에러를 던진다', async () => {
    await expect(login('admin@yologram.link', 'wrongpassword')).rejects.toThrow()
  })

  it('입력값 검증 실패(400) 시 에러를 던진다', async () => {
    await expect(login('', '')).rejects.toThrow()
  })
})

describe('validateToken', () => {
  it('유효한 토큰이면 어드민 정보를 반환한다', async () => {
    getDefaultStore().set(authAtom, validAuth)

    const result = await validateToken()

    expect(result).toEqual({
      uid: 1,
      email: 'admin@yologram.link',
      name: '관리자',
    })
  })

  it('만료된 토큰이면 에러를 던진다', async () => {
    getDefaultStore().set(authAtom, { ...validAuth, accessToken: 'expired-token' })

    await expect(validateToken()).rejects.toThrow()
  })

  it('토큰이 없으면 에러를 던진다', async () => {
    await expect(validateToken()).rejects.toThrow()
  })
})

describe('logout', () => {
  it('로그아웃 성공 시 에러 없이 완료된다', async () => {
    getDefaultStore().set(authAtom, validAuth)

    await expect(logout()).resolves.toBeUndefined()
  })

  it('토큰이 없으면 에러를 던진다', async () => {
    await expect(logout()).rejects.toThrow()
  })
})

describe('createAdminUser', () => {
  it('어드민 생성 성공 시 uid를 반환한다', async () => {
    getDefaultStore().set(authAtom, validAuth)

    const result = await createAdminUser({
      email: 'new-admin@yologram.link',
      name: '신규관리자',
      password: 'password123!',
    })

    expect(result).toEqual({ uid: 2 })
  })

  it('이메일 중복 시 에러를 던진다', async () => {
    getDefaultStore().set(authAtom, validAuth)

    await expect(
      createAdminUser({
        email: 'duplicate@yologram.link',
        name: '신규관리자',
        password: 'password123!',
      }),
    ).rejects.toThrow()
  })

  it('토큰이 없으면 에러를 던진다', async () => {
    await expect(
      createAdminUser({
        email: 'new-admin@yologram.link',
        name: '신규관리자',
        password: 'password123!',
      }),
    ).rejects.toThrow()
  })
})
