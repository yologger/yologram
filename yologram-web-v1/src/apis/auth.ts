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

export interface LoginResponse {
  uid: number
  accessToken: string
  email: string
  name: string
  nickname: string
}

export interface ValidateTokenResponse {
  uid: number
  email: string
  name: string
  nickname: string
}

export async function login(email: string, password: string): Promise<AuthState> {
  const response = await api.post<{ data: LoginResponse }>('/api/v1/ums/auth/login', { email, password })
  const { uid, accessToken, email: resEmail, name, nickname } = response.data.data
  return { uid, accessToken, email: resEmail, name, nickname }
}

export async function validateToken(): Promise<ValidateTokenResponse> {
  const response = await api.post<{ data: ValidateTokenResponse }>('/api/v1/ums/auth/validate-token')
  return response.data.data
}

export async function logout(): Promise<void> {
  await api.post('/api/v1/ums/auth/logout')
}
