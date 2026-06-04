'use client'

import { useMutation } from '@tanstack/react-query'
import { useRouter } from 'next/navigation'
import { useSetAtom } from 'jotai'
import { message } from 'antd'
import { authAtom } from '../stores/auth'
import { logout } from '../apis/auth'

export default function useLogoutMutation() {
  const router = useRouter()
  const setAuth = useSetAtom(authAtom)

  return useMutation({
    mutationFn: () => logout(),
    onSuccess: () => {
      setAuth(null)
      message.success('로그아웃 되었습니다.')
      router.push('/')
    },
    onError: () => {
      setAuth(null)
      message.success('로그아웃 되었습니다.')
      router.push('/')
    },
  })
}
