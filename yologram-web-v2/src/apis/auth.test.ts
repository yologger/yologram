import { describe, it, expect, beforeAll, afterAll, afterEach } from 'vitest'
import { getDefaultStore } from 'jotai'
import { server } from '../test/server'
import { authAtom } from '../stores/auth'
import { join, login, validateToken, logout, getMe, updateProfile, changePassword } from './auth'

const store = getDefaultStore()

beforeAll(() => server.listen())
afterEach(() => {
  server.resetHandlers()
  store.set(authAtom, null)
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
      name: '테스트',
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
  it('유효한 토큰으로 검증 성공한다', async () => {
    store.set(authAtom, {
      uid: 1,
      email: 'test@yologram.link',
      name: '테스트',
      nickname: 'tester',
      accessToken: 'valid-token',
    })

    const result = await validateToken()

    expect(result).toEqual({
      uid: 1,
      email: 'test@yologram.link',
      name: '테스트',
      nickname: 'tester',
    })
  })

  it('토큰 없으면 에러를 던진다', async () => {
    await expect(validateToken()).rejects.toThrow()
  })
})

describe('logout', () => {
  it('로그아웃 요청이 성공한다', async () => {
    store.set(authAtom, {
      uid: 1,
      email: 'test@yologram.link',
      name: '테스트',
      nickname: 'tester',
      accessToken: 'valid-token',
    })

    await expect(logout()).resolves.toBeUndefined()
  })
})

describe('getMe', () => {
  it('인증된 상태에서 유저 정보를 반환한다', async () => {
    store.set(authAtom, {
      uid: 1,
      accessToken: 'valid-token',
      email: 'test@yologram.link',
      name: '테스트',
      nickname: 'tester',
    })

    const result = await getMe()

    expect(result).toEqual({
      uid: 1,
      email: 'test@yologram.link',
      name: '테스트',
      nickname: 'tester',
      avatar: null,
      type: 'DEFAULT',
      joinedDate: '2025-01-01T00:00:00',
    })
  })

  it('인증되지 않은 상태에서 에러를 던진다', async () => {
    await expect(getMe()).rejects.toThrow()
  })

  it('만료된 토큰이면 에러를 던진다', async () => {
    store.set(authAtom, {
      uid: 1,
      accessToken: 'expired-token',
      email: 'test@yologram.link',
      name: '테스트',
      nickname: 'tester',
    })

    await expect(getMe()).rejects.toThrow()
  })
})

describe('updateProfile', () => {
  it('회원정보 수정 성공 시 유저 정보를 반환한다', async () => {
    store.set(authAtom, {
      uid: 1,
      accessToken: 'valid-token',
      email: 'test@yologram.link',
      name: '테스트',
      nickname: 'tester',
    })

    const result = await updateProfile({ nickname: 'new-nickname' })

    expect(result).toEqual({
      uid: 1,
      email: 'test@yologram.link',
      name: '테스트',
      nickname: 'new-nickname',
      avatar: null,
      type: 'DEFAULT',
      joinedDate: '2025-01-01T00:00:00',
    })
  })

  it('인증되지 않은 상태에서 에러를 던진다', async () => {
    await expect(updateProfile({ nickname: 'new-nickname' })).rejects.toThrow()
  })

  it('만료된 토큰이면 에러를 던진다', async () => {
    store.set(authAtom, {
      uid: 1,
      accessToken: 'expired-token',
      email: 'test@yologram.link',
      name: '테스트',
      nickname: 'tester',
    })

    await expect(updateProfile({ nickname: 'new-nickname' })).rejects.toThrow()
  })
})

describe('changePassword', () => {
  it('비밀번호 변경 성공 시 에러 없이 완료된다', async () => {
    store.set(authAtom, {
      uid: 1,
      accessToken: 'valid-token',
      email: 'test@yologram.link',
      name: '테스트',
      nickname: 'tester',
    })

    await expect(
      changePassword({ currentPassword: 'password123', newPassword: 'newpass1234' }),
    ).resolves.toBeUndefined()
  })

  it('현재 비밀번호 불일치 시 에러를 던진다', async () => {
    store.set(authAtom, {
      uid: 1,
      accessToken: 'valid-token',
      email: 'test@yologram.link',
      name: '테스트',
      nickname: 'tester',
    })

    await expect(
      changePassword({ currentPassword: 'wrongpassword', newPassword: 'newpass1234' }),
    ).rejects.toThrow()
  })

  it('인증되지 않은 상태에서 에러를 던진다', async () => {
    await expect(
      changePassword({ currentPassword: 'password123', newPassword: 'newpass1234' }),
    ).rejects.toThrow()
  })
})
