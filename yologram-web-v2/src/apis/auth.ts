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

interface LoginResponse {
  uid: number
  accessToken: string
  email: string
  name: string
  nickname: string
}

interface ValidateTokenResponse {
  uid: number
  email: string
  name: string
  nickname: string
}

export async function join(request: JoinRequest): Promise<JoinResponse> {
  const response = await api.post<{ data: JoinResponse }>('/api/v2/ums/user/join', request)
  return response.data.data
}

export async function login(email: string, password: string): Promise<AuthState> {
  const response = await api.post<{ data: LoginResponse }>('/api/v2/ums/auth/login', { email, password })
  const { uid, accessToken, email: resEmail, name, nickname } = response.data.data
  return { uid, accessToken, email: resEmail, name, nickname }
}

export async function validateToken(): Promise<ValidateTokenResponse> {
  const response = await api.post<{ data: ValidateTokenResponse }>('/api/v2/ums/auth/validate-token')
  return response.data.data
}

export async function logout(): Promise<void> {
  await api.post('/api/v2/ums/auth/logout')
}

export interface UserMeResponse {
  uid: number
  email: string
  name: string
  nickname: string
  avatar: string | null
  type: string
  joinedDate: string
}

export async function getMe(): Promise<UserMeResponse> {
  const response = await api.get<{ data: UserMeResponse }>('/api/v2/ums/user/me')
  return response.data.data
}
