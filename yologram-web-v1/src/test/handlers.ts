import { http, HttpResponse } from 'msw'

export const handlers = [
  http.post('http://localhost:5001/api/v1/ums/user/join', async ({ request }) => {
    const body = await request.json() as { email: string; name: string; nickname: string; password: string }

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

  http.post('http://localhost:5001/api/v1/ums/auth/email-verification/send', async ({ request }) => {
    const body = await request.json() as { email: string }

    if (body.email === 'duplicate@yologram.link') {
      return HttpResponse.json(
        { errorMessage: '이미 가입된 이메일입니다.', errorCode: 'USER_DUPLICATE' },
        { status: 409 },
      )
    }

    return new HttpResponse(null, { status: 204 })
  }),

  http.post('http://localhost:5001/api/v1/ums/auth/email-verification/verify', async ({ request }) => {
    const body = await request.json() as { email: string; code: string }

    if (body.code !== '123456') {
      return HttpResponse.json(
        { errorMessage: '인증 코드가 일치하지 않습니다.', errorCode: 'EMAIL_VERIFICATION_INVALID' },
        { status: 400 },
      )
    }

    return new HttpResponse(null, { status: 204 })
  }),

  http.post('http://localhost:5001/api/v1/ums/auth/password-reset/send', async ({ request }) => {
    const body = await request.json() as { email: string }

    if (body.email === 'notfound@yologram.link') {
      return HttpResponse.json(
        { errorMessage: '사용자를 찾을 수 없습니다.', errorCode: 'USER_NOT_FOUND' },
        { status: 404 },
      )
    }

    return new HttpResponse(null, { status: 204 })
  }),

  http.post('http://localhost:5001/api/v1/ums/auth/password-reset/verify', async ({ request }) => {
    const body = await request.json() as { email: string; code: string }

    if (body.code !== '123456') {
      return HttpResponse.json(
        { errorMessage: '인증 코드가 일치하지 않습니다.', errorCode: 'PASSWORD_RESET_INVALID' },
        { status: 400 },
      )
    }

    return new HttpResponse(null, { status: 204 })
  }),

  http.post('http://localhost:5001/api/v1/ums/auth/password-reset/confirm', async () => {
    return new HttpResponse(null, { status: 204 })
  }),

  http.post('http://localhost:5001/api/v1/ums/auth/login', async ({ request }) => {
    const body = await request.json() as { email: string; password: string }

    if (body.email === 'notfound@yologram.link') {
      return HttpResponse.json(
        { errorMessage: '존재하지 않는 사용자입니다.', errorCode: 'USER_NOT_FOUND' },
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
        name: '테스터',
        nickname: 'tester',
      },
    })
  }),

  http.post('http://localhost:5001/api/v1/ums/auth/validate-token', ({ request }) => {
    const authHeader = request.headers.get('Authorization')

    if (!authHeader || !authHeader.startsWith('Bearer ') || authHeader.substring(7) === 'expired-token') {
      return HttpResponse.json(
        { errorMessage: '유효하지 않은 토큰입니다.', errorCode: 'AUTH_TOKEN_INVALID' },
        { status: 401 },
      )
    }

    return HttpResponse.json({
      data: {
        uid: 1,
        email: 'test@yologram.link',
        name: '테스터',
        nickname: 'tester',
      },
    })
  }),

  http.get('http://localhost:5001/api/v1/ums/user/me', ({ request }) => {
    const authHeader = request.headers.get('Authorization')

    if (!authHeader || !authHeader.startsWith('Bearer ') || authHeader.substring(7) === 'expired-token') {
      return HttpResponse.json(
        { errorMessage: '유효하지 않은 토큰입니다.', errorCode: 'AUTH_TOKEN_INVALID' },
        { status: 401 },
      )
    }

    return HttpResponse.json({
      data: {
        uid: 1,
        email: 'test@yologram.link',
        name: '테스터',
        nickname: 'tester',
        avatar: null,
        type: 'DEFAULT',
        joinedDate: '2025-01-01T00:00:00',
      },
    })
  }),

  http.patch('http://localhost:5001/api/v1/ums/user/me', async ({ request }) => {
    const authHeader = request.headers.get('Authorization')

    if (!authHeader || !authHeader.startsWith('Bearer ') || authHeader.substring(7) === 'expired-token') {
      return HttpResponse.json(
        { errorMessage: '유효하지 않은 토큰입니다.', errorCode: 'AUTH_TOKEN_INVALID' },
        { status: 401 },
      )
    }

    const body = await request.json() as { nickname: string }

    return HttpResponse.json({
      data: {
        uid: 1,
        email: 'test@yologram.link',
        name: '테스터',
        nickname: body.nickname,
        avatar: null,
        type: 'DEFAULT',
        joinedDate: '2025-01-01T00:00:00',
      },
    })
  }),

  http.patch('http://localhost:5001/api/v1/ums/user/me/password', async ({ request }) => {
    const authHeader = request.headers.get('Authorization')

    if (!authHeader || !authHeader.startsWith('Bearer ') || authHeader.substring(7) === 'expired-token') {
      return HttpResponse.json(
        { errorMessage: '유효하지 않은 토큰입니다.', errorCode: 'AUTH_TOKEN_INVALID' },
        { status: 401 },
      )
    }

    const body = await request.json() as { currentPassword: string; newPassword: string }

    if (body.currentPassword === 'wrongpassword') {
      return HttpResponse.json(
        { errorMessage: '비밀번호가 올바르지 않습니다.', errorCode: 'AUTH_WRONG_PASSWORD' },
        { status: 401 },
      )
    }

    return new HttpResponse(null, { status: 204 })
  }),

  http.delete('http://localhost:5001/api/v1/ums/user/me', ({ request }) => {
    const authHeader = request.headers.get('Authorization')

    if (!authHeader || !authHeader.startsWith('Bearer ')) {
      return HttpResponse.json(
        { errorMessage: '유효하지 않은 토큰입니다.', errorCode: 'AUTH_TOKEN_INVALID' },
        { status: 401 },
      )
    }

    return new HttpResponse(null, { status: 204 })
  }),

  http.post('http://localhost:5001/api/v1/ums/auth/logout', ({ request }) => {
    const authHeader = request.headers.get('Authorization')

    if (!authHeader || !authHeader.startsWith('Bearer ')) {
      return HttpResponse.json(
        { errorMessage: '유효하지 않은 토큰입니다.', errorCode: 'AUTH_TOKEN_INVALID' },
        { status: 401 },
      )
    }

    return new HttpResponse(null, { status: 204 })
  }),

  http.get('http://localhost:5001/api/v1/cms/:section/categories', ({ params }) => {
    const section = String(params.section).toUpperCase()
    const categories: Record<string, { id: number; name: string; sortOrder: number }[]> = {
      TECH: [
        { id: 1, name: 'Frontend', sortOrder: 1 },
        { id: 2, name: 'Backend', sortOrder: 2 },
        { id: 3, name: 'AI/ML', sortOrder: 3 },
        { id: 7, name: '기타', sortOrder: 7 },
      ],
      INVEST: [
        { id: 8, name: '국내주식', sortOrder: 1 },
        { id: 9, name: '해외주식', sortOrder: 2 },
      ],
      POLITICS: [
        { id: 14, name: '국내정치', sortOrder: 1 },
        { id: 16, name: '정책', sortOrder: 3 },
      ],
    }

    if (!categories[section]) {
      return HttpResponse.json(
        { errorMessage: '유효하지 않은 섹션입니다.', errorCode: 'INVALID_SECTION' },
        { status: 400 },
      )
    }

    return HttpResponse.json({ data: categories[section] })
  }),

  http.post('http://localhost:5001/api/v1/pms/:section/posts', async ({ request }) => {
    const body = await request.json() as { content?: string; categoryIds?: number[] }

    if (!body.content || body.content.trim().length === 0) {
      return HttpResponse.json(
        { errorMessage: '내용을 입력해주세요.', errorCode: 'VALIDATION_ERROR' },
        { status: 400 },
      )
    }
    if (!body.categoryIds || body.categoryIds.length < 1 || body.categoryIds.length > 3) {
      return HttpResponse.json(
        { errorMessage: '카테고리는 1~3개 선택해주세요.', errorCode: 'VALIDATION_ERROR' },
        { status: 400 },
      )
    }

    return HttpResponse.json({ data: { id: 9999 } }, { status: 201 })
  }),
]
