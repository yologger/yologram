import { useMutation } from '@tanstack/react-query'
import { deleteAdminUser } from '../apis/adminUsers'

export default function useDeleteAdminUserMutation() {
  return useMutation({
    mutationFn: (uid: number) => deleteAdminUser(uid),
  })
}
