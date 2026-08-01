import { useMutation } from '@tanstack/react-query'
import { updateAdminUserStatus } from '../apis/adminUsers'

export default function useUpdateAdminUserStatusMutation() {
  return useMutation({
    mutationFn: ({ uid, status }: { uid: number; status: 'ACTIVE' | 'INACTIVE' }) =>
      updateAdminUserStatus(uid, status),
  })
}
