import { useMutation } from '@tanstack/react-query'
import { createAdminUser, type AdminUserCreateRequest } from '../apis/auth'

export default function useCreateAdminUserMutation() {
  return useMutation({
    mutationFn: (request: AdminUserCreateRequest) => createAdminUser(request),
  })
}
