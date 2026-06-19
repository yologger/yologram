import { describe, it, expect, beforeAll, afterAll, afterEach } from 'vitest'
import { server } from '../test/server'
import { getCategories } from './cms'

beforeAll(() => server.listen())
afterEach(() => server.resetHandlers())
afterAll(() => server.close())

describe('getCategories', () => {
  it('section별 카테고리 목록을 반환한다', async () => {
    const result = await getCategories('tech')

    expect(result).toEqual([
      { id: 1, name: 'Frontend', sortOrder: 1 },
      { id: 2, name: 'Backend', sortOrder: 2 },
      { id: 3, name: 'AI/ML', sortOrder: 3 },
    ])
  })

  it('대문자 section도 동일하게 조회한다', async () => {
    const result = await getCategories('INVEST')

    expect(result.map((c) => c.name)).toEqual(['국내주식', '해외주식'])
  })

  it('유효하지 않은 section이면 에러를 던진다', async () => {
    await expect(getCategories('unknown')).rejects.toThrow()
  })
})
