import { http, HttpResponse } from 'msw'

/** 어드민 계정 목킹 데이터 — uid 1은 로그인 테스트 어드민 본인. 총 12명(기본 size 10 기준 2페이지) */
export const mockAdminUsers = [
  {
    uid: 1,
    email: 'admin@yologram.link',
    name: '관리자',
    role: 'OWNER',
    status: 'ACTIVE',
    joinedDate: '2026-06-01T09:00:00',
  },
  {
    uid: 2,
    email: 'second@yologram.link',
    name: '부관리자',
    role: 'ADMIN',
    status: 'INACTIVE',
    joinedDate: '2026-06-15T09:00:00',
  },
  ...Array.from({ length: 10 }, (_, index) => ({
    uid: index + 3,
    email: `admin${index + 3}@yologram.link`,
    name: `운영자${index + 3}`,
    role: 'ADMIN',
    status: 'ACTIVE',
    joinedDate: '2026-06-20T09:00:00',
  })),
]

/** 어드민 목록 페이지 응답 생성 (0-based page) */
export function buildAdminUsersPage(source: typeof mockAdminUsers, page: number, size: number) {
  const totalCount = source.length
  const totalPages = Math.max(1, Math.ceil(totalCount / size))
  return {
    data: source.slice(page * size, page * size + size),
    page,
    size,
    totalPages,
    totalCount,
    first: page === 0,
    last: page >= totalPages - 1,
  }
}

/** 뉴스 소스 목킹 데이터 — 테스트에서 목록·수정·삭제 대상 기준값으로 사용 */
export const mockNewsSources = [
  {
    id: 1,
    name: '우아한형제들 기술블로그',
    url: 'https://techblog.woowahan.com/feed',
    isActive: true,
    createdAt: '2026-07-01T10:00:00',
    modifiedDate: '2026-07-01T10:00:00',
  },
  {
    id: 2,
    name: '카카오 기술블로그',
    url: 'https://tech.kakao.com/feed',
    isActive: false,
    createdAt: '2026-07-02T10:00:00',
    modifiedDate: '2026-07-02T10:00:00',
  },
]

function unauthorized() {
  return HttpResponse.json(
    { errorMessage: '유효하지 않은 토큰입니다.', errorCode: 'AUTH_INVALID_TOKEN' },
    { status: 401 },
  )
}

function hasBearer(request: Request) {
  const authHeader = request.headers.get('Authorization')
  return !!authHeader && authHeader.startsWith('Bearer ')
}

export const handlers = [
  http.post('http://localhost:5001/api/v1/ums/admin/auth/login', async ({ request }) => {
    const body = await request.json() as { email?: string; password?: string }

    if (!body.email || !body.password) {
      return HttpResponse.json(
        { errorMessage: '입력값이 올바르지 않습니다.', errorCode: 'VALIDATION_ERROR' },
        { status: 400 },
      )
    }

    if (body.email === 'notfound@yologram.link') {
      return HttpResponse.json(
        { errorMessage: '존재하지 않는 어드민입니다.', errorCode: 'ADMIN_USER_NOT_FOUND' },
        { status: 404 },
      )
    }

    if (body.email === 'inactive@yologram.link') {
      return HttpResponse.json(
        { errorMessage: '비활성화된 계정입니다.', errorCode: 'ADMIN_USER_INACTIVE' },
        { status: 403 },
      )
    }

    if (body.password === 'wrongpassword') {
      return HttpResponse.json(
        { errorMessage: '비밀번호가 일치하지 않습니다.', errorCode: 'AUTH_WRONG_PASSWORD' },
        { status: 401 },
      )
    }

    return HttpResponse.json({
      data: {
        uid: 1,
        accessToken: 'mock-access-token',
        email: body.email,
        name: '관리자',
        role: 'OWNER',
      },
    })
  }),

  http.post('http://localhost:5001/api/v1/ums/admin/auth/validate-token', ({ request }) => {
    const authHeader = request.headers.get('Authorization')

    if (!authHeader || !authHeader.startsWith('Bearer ') || authHeader.substring(7) === 'expired-token') {
      return HttpResponse.json(
        { errorMessage: '유효하지 않은 토큰입니다.', errorCode: 'AUTH_INVALID_TOKEN' },
        { status: 401 },
      )
    }

    // 비활성화된 계정 토큰
    if (authHeader.substring(7) === 'inactive-token') {
      return HttpResponse.json(
        { errorMessage: '비활성화된 계정입니다.', errorCode: 'ADMIN_USER_INACTIVE' },
        { status: 403 },
      )
    }

    return HttpResponse.json({
      data: {
        uid: 1,
        email: 'admin@yologram.link',
        name: '관리자',
        role: 'OWNER',
      },
    })
  }),

  http.post('http://localhost:5001/api/v1/ums/admin/auth/logout', ({ request }) => {
    const authHeader = request.headers.get('Authorization')

    if (!authHeader || !authHeader.startsWith('Bearer ')) {
      return HttpResponse.json(
        { errorMessage: '유효하지 않은 토큰입니다.', errorCode: 'AUTH_INVALID_TOKEN' },
        { status: 401 },
      )
    }

    return new HttpResponse(null, { status: 204 })
  }),

  http.post('http://localhost:5001/api/v1/ums/admin/admin-users', async ({ request }) => {
    const authHeader = request.headers.get('Authorization')

    if (!authHeader || !authHeader.startsWith('Bearer ')) {
      return HttpResponse.json(
        { errorMessage: '유효하지 않은 토큰입니다.', errorCode: 'AUTH_INVALID_TOKEN' },
        { status: 401 },
      )
    }

    const body = await request.json() as { email?: string; name?: string; password?: string }

    if (!body.email || !body.name || !body.password) {
      return HttpResponse.json(
        { errorMessage: '입력값이 올바르지 않습니다.', errorCode: 'VALIDATION_ERROR' },
        { status: 400 },
      )
    }

    if (body.email === 'duplicate@yologram.link') {
      return HttpResponse.json(
        { errorMessage: '이미 등록된 이메일입니다.', errorCode: 'ADMIN_USER_DUPLICATE' },
        { status: 409 },
      )
    }

    return HttpResponse.json({ data: { uid: 2 } }, { status: 201 })
  }),

  http.get('http://localhost:5001/api/v1/ums/admin/admin-users', ({ request }) => {
    if (!hasBearer(request)) return unauthorized()

    const url = new URL(request.url)
    const page = Number(url.searchParams.get('page') ?? 0)
    const size = Number(url.searchParams.get('size') ?? 10)

    return HttpResponse.json(buildAdminUsersPage(mockAdminUsers, page, size))
  }),

  http.patch('http://localhost:5001/api/v1/ums/admin/admin-users/:id/status', async ({ request, params }) => {
    const authHeader = request.headers.get('Authorization')
    if (!authHeader || !authHeader.startsWith('Bearer ')) return unauthorized()

    // 'admin-token' = ADMIN 권한 토큰 — 상태 변경은 OWNER 전용
    if (authHeader.substring(7) === 'admin-token') {
      return HttpResponse.json(
        { errorMessage: 'OWNER만 가능한 작업입니다.', errorCode: 'ADMIN_ROLE_FORBIDDEN' },
        { status: 403 },
      )
    }

    const id = Number(params.id)

    // uid 1 = OWNER 계정
    if (id === 1) {
      return HttpResponse.json(
        { errorMessage: 'OWNER 계정은 변경할 수 없습니다.', errorCode: 'ADMIN_USER_OWNER_IMMUTABLE' },
        { status: 400 },
      )
    }

    if (id === 99999) {
      return HttpResponse.json(
        { errorMessage: '어드민을 찾을 수 없습니다.', errorCode: 'ADMIN_USER_NOT_FOUND' },
        { status: 404 },
      )
    }

    const body = await request.json() as { status?: string }
    const base = mockAdminUsers.find((adminUser) => adminUser.uid === id) ?? mockAdminUsers[1]

    return HttpResponse.json({ data: { ...base, uid: id, status: body.status } })
  }),

  http.delete('http://localhost:5001/api/v1/ums/admin/admin-users/:id', ({ request, params }) => {
    if (!hasBearer(request)) return unauthorized()

    const id = Number(params.id)

    // uid 1 = 로그인 테스트 어드민 본인
    if (id === 1) {
      return HttpResponse.json(
        { errorMessage: '자기 자신은 삭제할 수 없습니다.', errorCode: 'ADMIN_USER_SELF_DELETE' },
        { status: 400 },
      )
    }

    if (id === 99999) {
      return HttpResponse.json(
        { errorMessage: '어드민을 찾을 수 없습니다.', errorCode: 'ADMIN_USER_NOT_FOUND' },
        { status: 404 },
      )
    }

    return new HttpResponse(null, { status: 204 })
  }),

  http.get('http://localhost:5001/api/v1/news/admin/tech/sources', ({ request }) => {
    if (!hasBearer(request)) return unauthorized()

    return HttpResponse.json({ data: mockNewsSources })
  }),

  http.post('http://localhost:5001/api/v1/news/admin/tech/sources', async ({ request }) => {
    if (!hasBearer(request)) return unauthorized()

    const body = await request.json() as { name?: string; url?: string; isActive?: boolean }

    if (!body.name || body.name.length > 100 || !body.url || body.url.length > 500 || !/^https?:\/\/.+/.test(body.url)) {
      return HttpResponse.json(
        { errorMessage: '입력값이 올바르지 않습니다.', errorCode: 'VALIDATION_ERROR' },
        { status: 400 },
      )
    }

    if (body.url === 'https://duplicate.example.com/feed') {
      return HttpResponse.json(
        { errorMessage: '이미 등록된 URL입니다.', errorCode: 'NEWS_SOURCE_DUPLICATE' },
        { status: 409 },
      )
    }

    return HttpResponse.json(
      {
        data: {
          id: 3,
          name: body.name,
          url: body.url,
          isActive: body.isActive ?? true,
          createdAt: '2026-07-03T10:00:00',
          modifiedDate: '2026-07-03T10:00:00',
        },
      },
      { status: 201 },
    )
  }),

  http.patch('http://localhost:5001/api/v1/news/admin/tech/sources/:id', async ({ request, params }) => {
    if (!hasBearer(request)) return unauthorized()

    const id = Number(params.id)
    if (id === 99999) {
      return HttpResponse.json(
        { errorMessage: '소스를 찾을 수 없습니다.', errorCode: 'NEWS_SOURCE_NOT_FOUND' },
        { status: 404 },
      )
    }

    const body = await request.json() as { name?: string; url?: string; isActive?: boolean }

    if (body.url === 'https://duplicate.example.com/feed') {
      return HttpResponse.json(
        { errorMessage: '이미 등록된 URL입니다.', errorCode: 'NEWS_SOURCE_DUPLICATE' },
        { status: 409 },
      )
    }

    const base = mockNewsSources.find((source) => source.id === id) ?? mockNewsSources[0]

    return HttpResponse.json({
      data: {
        ...base,
        id,
        name: body.name ?? base.name,
        url: body.url ?? base.url,
        isActive: body.isActive ?? base.isActive,
        modifiedDate: '2026-07-04T10:00:00',
      },
    })
  }),

  http.delete('http://localhost:5001/api/v1/news/admin/tech/sources/:id', ({ request, params }) => {
    if (!hasBearer(request)) return unauthorized()

    if (Number(params.id) === 99999) {
      return HttpResponse.json(
        { errorMessage: '소스를 찾을 수 없습니다.', errorCode: 'NEWS_SOURCE_NOT_FOUND' },
        { status: 404 },
      )
    }

    return new HttpResponse(null, { status: 204 })
  }),
]
