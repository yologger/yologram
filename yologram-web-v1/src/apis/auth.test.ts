import { describe, it, expect, beforeAll, afterAll, afterEach } from 'vitest'
import { server } from '../test/server'
import { join, login, logout, validateToken, getMe, updateProfile, changePassword, sendVerificationCode, verifyEmail, sendPasswordResetCode, verifyPasswordResetCode, confirmPasswordReset } from './auth'
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

describe('sendVerificationCode', () => {
  it('발송 성공 시 에러 없이 완료된다', async () => {
    await expect(sendVerificationCode('new@yologram.link')).resolves.toBeUndefined()
  })

  it('이미 가입된 이메일이면 에러를 던진다', async () => {
    await expect(sendVerificationCode('duplicate@yologram.link')).rejects.toThrow()
  })
})

describe('verifyEmail', () => {
  it('올바른 코드면 에러 없이 완료된다', async () => {
    await expect(verifyEmail('new@yologram.link', '123456')).resolves.toBeUndefined()
  })

  it('잘못된 코드면 에러를 던진다', async () => {
    await expect(verifyEmail('new@yologram.link', '000000')).rejects.toThrow()
  })
})

describe('sendPasswordResetCode', () => {
  it('발송 성공 시 에러 없이 완료된다', async () => {
    await expect(sendPasswordResetCode('test@yologram.link')).resolves.toBeUndefined()
  })

  it('가입되지 않은 이메일이면 에러를 던진다', async () => {
    await expect(sendPasswordResetCode('notfound@yologram.link')).rejects.toThrow()
  })
})

describe('verifyPasswordResetCode', () => {
  it('올바른 코드면 에러 없이 완료된다', async () => {
    await expect(verifyPasswordResetCode('test@yologram.link', '123456')).resolves.toBeUndefined()
  })

  it('잘못된 코드면 에러를 던진다', async () => {
    await expect(verifyPasswordResetCode('test@yologram.link', '000000')).rejects.toThrow()
  })
})

describe('confirmPasswordReset', () => {
  it('성공 시 에러 없이 완료된다', async () => {
    await expect(confirmPasswordReset('test@yologram.link', '123456', 'newpass1234')).resolves.toBeUndefined()
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

describe('getMe', () => {
  it('인증된 상태에서 유저 정보를 반환한다', async () => {
    getDefaultStore().set(authAtom, {
      uid: 1,
      accessToken: 'valid-token',
      email: 'test@yologram.link',
      name: '테스터',
      nickname: 'tester',
    })

    const result = await getMe()

    expect(result).toEqual({
      uid: 1,
      email: 'test@yologram.link',
      name: '테스터',
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
    getDefaultStore().set(authAtom, {
      uid: 1,
      accessToken: 'expired-token',
      email: 'test@yologram.link',
      name: '테스터',
      nickname: 'tester',
    })

    await expect(getMe()).rejects.toThrow()
  })
})

describe('updateProfile', () => {
  it('닉네임 수정 성공 시 수정된 유저 정보를 반환한다', async () => {
    getDefaultStore().set(authAtom, {
      uid: 1,
      accessToken: 'valid-token',
      email: 'test@yologram.link',
      name: '테스터',
      nickname: 'tester',
    })

    const result = await updateProfile({ nickname: 'new-nickname' })

    expect(result).toEqual({
      uid: 1,
      email: 'test@yologram.link',
      name: '테스터',
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
    getDefaultStore().set(authAtom, {
      uid: 1,
      accessToken: 'expired-token',
      email: 'test@yologram.link',
      name: '테스터',
      nickname: 'tester',
    })

    await expect(updateProfile({ nickname: 'new-nickname' })).rejects.toThrow()
  })
})

describe('changePassword', () => {
  it('비밀번호 변경 성공 시 에러 없이 완료된다', async () => {
    getDefaultStore().set(authAtom, {
      uid: 1,
      accessToken: 'valid-token',
      email: 'test@yologram.link',
      name: '테스터',
      nickname: 'tester',
    })

    await expect(
      changePassword({ currentPassword: 'password123', newPassword: 'newpass1234' }),
    ).resolves.toBeUndefined()
  })

  it('현재 비밀번호 불일치 시 에러를 던진다', async () => {
    getDefaultStore().set(authAtom, {
      uid: 1,
      accessToken: 'valid-token',
      email: 'test@yologram.link',
      name: '테스터',
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
