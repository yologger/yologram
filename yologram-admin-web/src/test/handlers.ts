import { http, HttpResponse } from 'msw'

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

    return HttpResponse.json({
      data: {
        uid: 1,
        email: 'admin@yologram.link',
        name: '관리자',
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
        { errorMessage: '이미 등록된 어드민입니다.', errorCode: 'ADMIN_USER_DUPLICATE' },
        { status: 409 },
      )
    }

    return HttpResponse.json({ data: { uid: 2 } }, { status: 201 })
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
