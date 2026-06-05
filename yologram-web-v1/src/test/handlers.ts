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
]
