import { http, HttpResponse } from 'msw'

export const handlers = [
  http.post('http://localhost:5002/api/v2/ums/user/join', async ({ request }) => {
    const body = await request.json() as { email: string }

    if (body.email === 'duplicate@yologram.link') {
      return HttpResponse.json(
        { errorMessage: '이미 가입된 이메일입니다.', errorCode: 'USER_DUPLICATE' },
        { status: 409 },
      )
    }

    return HttpResponse.json(
      { data: { uid: 1 } },
      { status: 201 },
    )
  }),

  http.post('http://localhost:5002/api/v2/ums/auth/login', async ({ request }) => {
    const body = await request.json() as { email: string; password: string }

    if (body.email === 'notfound@yologram.link') {
      return HttpResponse.json(
        { errorMessage: '사용자를 찾을 수 없습니다.', errorCode: 'USER_NOT_FOUND' },
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
        name: '테스트',
        nickname: 'tester',
      },
    })
  }),

  http.post('http://localhost:5002/api/v2/ums/auth/validate-token', ({ request }) => {
    const auth = request.headers.get('Authorization')

    if (!auth || !auth.startsWith('Bearer ') || auth === 'Bearer ') {
      return HttpResponse.json(
        { errorMessage: '유효하지 않은 토큰입니다.', errorCode: 'AUTH_TOKEN_INVALID' },
        { status: 401 },
      )
    }

    return HttpResponse.json({
      data: {
        uid: 1,
        email: 'test@yologram.link',
        name: '테스트',
        nickname: 'tester',
      },
    })
  }),

  http.post('http://localhost:5002/api/v2/ums/auth/logout', ({ request }) => {
    const auth = request.headers.get('Authorization')

    if (!auth || !auth.startsWith('Bearer ')) {
      return HttpResponse.json(
        { errorMessage: '유효하지 않은 토큰입니다.', errorCode: 'AUTH_TOKEN_INVALID' },
        { status: 401 },
      )
    }

    return new HttpResponse(null, { status: 204 })
  }),
]
