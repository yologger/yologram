import { describe, it, expect, beforeAll, afterAll, afterEach } from 'vitest'
import { server } from '../test/server'
import { join } from './auth'

beforeAll(() => server.listen())
afterEach(() => server.resetHandlers())
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
