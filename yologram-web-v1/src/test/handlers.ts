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
        { errorMessage: '인증 코드가 일치하지 않습니다.', errorCode: 'USER_EMAIL_VERIFICATION_INVALID' },
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
        { errorMessage: '인증 코드가 일치하지 않습니다.', errorCode: 'USER_PASSWORD_RESET_INVALID' },
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
        { errorMessage: '유효하지 않은 토큰입니다.', errorCode: 'AUTH_INVALID_TOKEN' },
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
        { errorMessage: '유효하지 않은 토큰입니다.', errorCode: 'AUTH_INVALID_TOKEN' },
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
        { errorMessage: '유효하지 않은 토큰입니다.', errorCode: 'AUTH_INVALID_TOKEN' },
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
        { errorMessage: '유효하지 않은 토큰입니다.', errorCode: 'AUTH_INVALID_TOKEN' },
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
        { errorMessage: '유효하지 않은 토큰입니다.', errorCode: 'AUTH_INVALID_TOKEN' },
        { status: 401 },
      )
    }

    return new HttpResponse(null, { status: 204 })
  }),

  http.post('http://localhost:5001/api/v1/ums/auth/logout', ({ request }) => {
    const authHeader = request.headers.get('Authorization')

    if (!authHeader || !authHeader.startsWith('Bearer ')) {
      return HttpResponse.json(
        { errorMessage: '유효하지 않은 토큰입니다.', errorCode: 'AUTH_INVALID_TOKEN' },
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

  http.get('http://localhost:5001/api/v1/pms/:section/posts', ({ request, params }) => {
    const section = String(params.section).toUpperCase()
    if (!['TECH', 'INVEST', 'POLITICS'].includes(section)) {
      return HttpResponse.json(
        { errorMessage: '유효하지 않은 섹션입니다.', errorCode: 'INVALID_SECTION' },
        { status: 400 },
      )
    }

    const url = new URL(request.url)
    const cursor = url.searchParams.get('cursor')
    const categoryId = url.searchParams.get('categoryId')

    // 커서가 있으면 마지막 페이지(빈 결과)로 무한스크롤 종료
    if (cursor) {
      return HttpResponse.json({ data: [], nextCursor: null })
    }

    const all = [
      { id: 1050, section: 'TECH', author: { uid: 1, nickname: '테스터' }, title: '피드 첫 글', content: 'API 피드 본문 1', categoryIds: [1], likeCount: 3, commentCount: 1, createdAt: '2026-06-10T00:00:00' },
      { id: 1049, section: 'TECH', author: { uid: 2, nickname: '다른유저' }, content: 'API 피드 본문 2', categoryIds: [2], likeCount: 0, commentCount: 0, createdAt: '2026-06-09T00:00:00' },
    ]
    const data = categoryId ? all.filter((p) => p.categoryIds.includes(Number(categoryId))) : all

    return HttpResponse.json({ data, nextCursor: 'next-cursor' })
  }),

  http.get('http://localhost:5001/api/v1/pms/posts/me', ({ request }) => {
    const authHeader = request.headers.get('Authorization')
    if (!authHeader || !authHeader.startsWith('Bearer ')) {
      return HttpResponse.json(
        { errorMessage: '유효하지 않은 토큰입니다.', errorCode: 'AUTH_INVALID_TOKEN' },
        { status: 401 },
      )
    }

    const url = new URL(request.url)
    const cursor = url.searchParams.get('cursor')
    const section = url.searchParams.get('section')

    // 커서가 있으면 마지막 페이지(빈 결과)로 무한스크롤 종료
    if (cursor) {
      return HttpResponse.json({ data: [], nextCursor: null })
    }

    const all = [
      { id: 2001, section: 'TECH', author: { uid: 1, nickname: '테스터' }, content: '내가 쓴 기술 글', categoryIds: [1], likeCount: 3, commentCount: 1, createdAt: '2026-06-18T09:00:00' },
      { id: 2002, section: 'INVEST', author: { uid: 1, nickname: '테스터' }, content: '내가 쓴 투자 글', categoryIds: [9], likeCount: 5, commentCount: 2, createdAt: '2026-06-17T09:00:00' },
      { id: 2003, section: 'POLITICS', author: { uid: 1, nickname: '테스터' }, content: '내가 쓴 정치 글', categoryIds: [16], likeCount: 1, commentCount: 0, createdAt: '2026-06-16T09:00:00' },
    ]
    const data = section ? all.filter((p) => p.section.toLowerCase() === section.toLowerCase()) : all

    return HttpResponse.json({ data, nextCursor: 'next-cursor' })
  }),

  http.get('http://localhost:5001/api/v1/pms/:section/posts/:id', ({ params }) => {
    const id = Number(params.id)

    if (id === 99999) {
      return HttpResponse.json(
        { errorMessage: '게시글을 찾을 수 없습니다.', errorCode: 'POST_NOT_FOUND' },
        { status: 404 },
      )
    }

    // id 2 는 타인 글(uid 99), 그 외는 로그인 테스트 유저(uid 1) 본인 글
    const authorUid = id === 2 ? 99 : 1

    return HttpResponse.json({
      data: {
        id,
        section: 'TECH',
        author: { uid: authorUid, nickname: '테스터' },
        title: 'API 제목',
        content: 'API 본문 내용',
        categoryIds: [1],
        likeCount: 5,
        commentCount: 0,
        createdAt: '2026-01-01T00:00:00',
      },
    })
  }),

  http.patch('http://localhost:5001/api/v1/pms/:section/posts/:id', async ({ request }) => {
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

    return new HttpResponse(null, { status: 204 })
  }),

  http.post('http://localhost:5001/api/v1/comments/posts/:postId', async ({ request, params }) => {
    const authHeader = request.headers.get('Authorization')
    if (!authHeader || !authHeader.startsWith('Bearer ')) {
      return HttpResponse.json(
        { errorMessage: '유효하지 않은 토큰입니다.', errorCode: 'AUTH_INVALID_TOKEN' },
        { status: 401 },
      )
    }

    const postId = Number(params.postId)
    if (postId === 99999) {
      return HttpResponse.json(
        { errorMessage: '게시글을 찾을 수 없습니다.', errorCode: 'POST_NOT_FOUND' },
        { status: 404 },
      )
    }

    const body = await request.json() as { content?: string }
    if (!body.content || body.content.trim().length === 0) {
      return HttpResponse.json(
        { errorMessage: '내용을 입력해주세요.', errorCode: 'VALIDATION_ERROR' },
        { status: 400 },
      )
    }

    return HttpResponse.json({ data: { id: 5001 } }, { status: 201 })
  }),

  http.get('http://localhost:5001/api/v1/comments/posts/:postId', ({ request, params }) => {
    const postId = Number(params.postId)
    const url = new URL(request.url)
    const cursor = url.searchParams.get('cursor')
    const sort = url.searchParams.get('sort') ?? 'latest'

    // 커서가 있으면 마지막 페이지(빈 결과)로 무한스크롤 종료
    if (cursor) {
      return HttpResponse.json({ data: [], nextCursor: null })
    }

    // 최신순(latest): 최신이 위, 오래된순(oldest): 오래된 게 위
    const latest = [
      { id: 102, postId, author: { uid: 2, nickname: '다른유저' }, content: '최신 댓글', createdAt: '2026-06-20T12:00:00' },
      { id: 101, postId, author: { uid: 1, nickname: '테스터' }, content: '오래된 댓글', createdAt: '2026-06-19T12:00:00' },
    ]
    const data = sort === 'oldest' ? [...latest].reverse() : latest

    return HttpResponse.json({ data, nextCursor: 'next-cursor' })
  }),

  http.delete('http://localhost:5001/api/v1/pms/:section/posts/:id', ({ request, params }) => {
    const authHeader = request.headers.get('Authorization')
    if (!authHeader || !authHeader.startsWith('Bearer ')) {
      return HttpResponse.json(
        { errorMessage: '유효하지 않은 토큰입니다.', errorCode: 'AUTH_INVALID_TOKEN' },
        { status: 401 },
      )
    }

    const id = Number(params.id)

    if (id === 99999) {
      return HttpResponse.json(
        { errorMessage: '게시글을 찾을 수 없습니다.', errorCode: 'POST_NOT_FOUND' },
        { status: 404 },
      )
    }

    // id 2 는 타인 글 → 본인만 삭제 가능
    if (id === 2) {
      return HttpResponse.json(
        { errorMessage: '권한이 없습니다.', errorCode: 'POST_FORBIDDEN' },
        { status: 403 },
      )
    }

    return new HttpResponse(null, { status: 204 })
  }),
]
