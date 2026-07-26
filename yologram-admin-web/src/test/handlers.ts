import { http, HttpResponse } from 'msw'

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
]
