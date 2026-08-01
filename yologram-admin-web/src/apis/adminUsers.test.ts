import { describe, it, expect, beforeAll, beforeEach, afterEach, afterAll } from 'vitest'
import { getDefaultStore } from 'jotai'
import { server } from '../test/server'
import { mockAdminUsers } from '../test/handlers'
import { getAdminUsers, deleteAdminUser, updateAdminUserStatus } from './adminUsers'
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

describe('getAdminUsers', () => {
  it('첫 페이지(기본 page 0, size 10) 목록과 페이지 정보를 반환한다', async () => {
    const result = await getAdminUsers()

    expect(result.data).toEqual(mockAdminUsers.slice(0, 10))
    expect(result.page).toBe(0)
    expect(result.size).toBe(10)
    expect(result.totalCount).toBe(12)
    expect(result.totalPages).toBe(2)
    expect(result.first).toBe(true)
    expect(result.last).toBe(false)
  })

  it('두 번째 페이지(page 1)는 나머지 목록과 last=true를 반환한다', async () => {
    const result = await getAdminUsers(1)

    expect(result.data).toEqual(mockAdminUsers.slice(10))
    expect(result.page).toBe(1)
    expect(result.first).toBe(false)
    expect(result.last).toBe(true)
  })

  it('토큰이 없으면 에러를 던진다', async () => {
    getDefaultStore().set(authAtom, null)

    await expect(getAdminUsers()).rejects.toThrow()
  })
})

describe('updateAdminUserStatus', () => {
  it('상태 변경 성공 시 변경된 어드민을 반환한다', async () => {
    const result = await updateAdminUserStatus(2, 'ACTIVE')

    expect(result.uid).toBe(2)
    expect(result.status).toBe('ACTIVE')
  })

  it('OWNER 계정 변경(400)이면 에러를 던진다', async () => {
    await expect(updateAdminUserStatus(1, 'INACTIVE')).rejects.toThrow()
  })

  it('ADMIN 권한 토큰(403)이면 에러를 던진다', async () => {
    getDefaultStore().set(authAtom, { ...validAuth, role: 'ADMIN', accessToken: 'admin-token' })

    await expect(updateAdminUserStatus(2, 'INACTIVE')).rejects.toThrow()
  })

  it('존재하지 않는 어드민이면 에러를 던진다', async () => {
    await expect(updateAdminUserStatus(99999, 'ACTIVE')).rejects.toThrow()
  })
})

describe('deleteAdminUser', () => {
  it('삭제 성공 시 에러 없이 완료된다', async () => {
    await expect(deleteAdminUser(2)).resolves.toBeUndefined()
  })

  it('자기 자신 삭제(400)면 에러를 던진다', async () => {
    await expect(deleteAdminUser(1)).rejects.toThrow()
  })

  it('존재하지 않는 어드민이면 에러를 던진다', async () => {
    await expect(deleteAdminUser(99999)).rejects.toThrow()
  })
})
