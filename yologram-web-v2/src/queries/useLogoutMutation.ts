'use client'

import { useMutation } from '@tanstack/react-query'
import { logout } from '../apis/auth'

export default function useLogoutMutation() {
  return useMutation({
    mutationFn: () => logout(),
    onSuccess: () => {
      localStorage.removeItem('auth')
      window.location.href = '/'
    },
    onError: () => {
      localStorage.removeItem('auth')
      window.location.href = '/'
    },
  })
}
