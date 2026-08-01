import api from '../lib/api'

export type AdminUserStatus = 'ACTIVE' | 'INACTIVE' | 'DELETED'

export interface AdminUser {
  uid: number
  email: string
  name: string
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
