import type { AuthState } from '../stores/auth'

// TODO: API 서버 구축 후 실제 API 호출로 교체
// POST /api/ums/v1/auth/login
export async function login(email: string, password: string): Promise<AuthState> {
  if (email === 'test@yologram.link' && password === 'password') {
    return {
      uid: 1,
      email,
      nickname: 'yologram',
      accessToken: 'dummy-access-token',
    }
  }
  throw new Error('이메일 또는 비밀번호가 올바르지 않습니다.')
}

// POST /api/ums/v1/auth/logout
export async function logout(): Promise<void> {
  // 더미 로그아웃 처리
}
