import api from '../lib/api'
import type { AdminRole } from '../stores/auth'

export type AdminUserStatus = 'ACTIVE' | 'INACTIVE' | 'DELETED'

export interface AdminUser {
  uid: number
  email: string
  name: string
  role: AdminRole
  status: AdminUserStatus
  joinedDate: string
}

export interface AdminUsersPageResponse {
  data: AdminUser[]
  /** 0-based 페이지 번호 */
  page: number
  size: number
  totalPages: number
  totalCount: number
  first: boolean
  last: boolean
}

const BASE_PATH = '/api/v1/ums/admin/admin-users'

export async function getAdminUsers(page = 0, size = 10): Promise<AdminUsersPageResponse> {
  const response = await api.get<AdminUsersPageResponse>(BASE_PATH, { params: { page, size } })
  return response.data
}

export async function deleteAdminUser(uid: number): Promise<void> {
  await api.delete(`${BASE_PATH}/${uid}`)
}

/** 활성/비활성 상태 변경 — OWNER 전용 (DELETED는 hard delete라 대상 아님) */
export async function updateAdminUserStatus(
  uid: number,
  status: 'ACTIVE' | 'INACTIVE',
): Promise<AdminUser> {
  const response = await api.patch<{ data: AdminUser }>(`${BASE_PATH}/${uid}/status`, { status })
  return response.data.data
}
