import api from '../lib/api'
import type { AuthState } from '../stores/auth'

export interface JoinRequest {
  email: string
  name: string
  nickname: string
  password: string
}

export interface JoinResponse {
  uid: number
}

export async function join(request: JoinRequest): Promise<JoinResponse> {
  const response = await api.post<{ data: JoinResponse }>('/api/v1/ums/user/join', request)
  return response.data.data
}

// TODO: 로그인 API 구현 후 실제 API 호출로 교체
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

export async function logout(): Promise<void> {
  // TODO: 로그아웃 API 구현 후 교체
}
