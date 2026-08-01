import api from '../lib/api'
import type { AdminRole, AuthState } from '../stores/auth'

export interface LoginResponse {
  uid: number
  accessToken: string
  email: string
  name: string
  role: AdminRole
}

export interface ValidateTokenResponse {
  uid: number
  email: string
  name: string
  role: AdminRole
}

export async function login(email: string, password: string): Promise<AuthState> {
  const response = await api.post<{ data: LoginResponse }>('/api/v1/ums/admin/auth/login', { email, password })
  const { uid, accessToken, email: resEmail, name, role } = response.data.data
  return { uid, accessToken, email: resEmail, name, role }
}

export async function validateToken(): Promise<ValidateTokenResponse> {
  const response = await api.post<{ data: ValidateTokenResponse }>('/api/v1/ums/admin/auth/validate-token')
  return response.data.data
}

export async function logout(): Promise<void> {
  await api.post('/api/v1/ums/admin/auth/logout')
}

export interface AdminUserCreateRequest {
  email: string
  name: string
  password: string
}

export interface AdminUserCreateResponse {
  uid: number
}

export async function createAdminUser(request: AdminUserCreateRequest): Promise<AdminUserCreateResponse> {
  const response = await api.post<{ data: AdminUserCreateResponse }>('/api/v1/ums/admin/admin-users', request)
  return response.data.data
}
