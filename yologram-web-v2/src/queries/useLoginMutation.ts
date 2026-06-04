'use client'

import { useMutation } from '@tanstack/react-query'
import { useRouter } from 'next/navigation'
import { useSetAtom } from 'jotai'
import { message } from 'antd'
import { authAtom } from '../stores/auth'
import { login } from '../apis/auth'

export default function useLoginMutation() {
  const router = useRouter()
  const setAuth = useSetAtom(authAtom)

  return useMutation({
    mutationFn: ({ email, password }: { email: string; password: string }) => login(email, password),
    onSuccess: (data) => {
      setAuth(data)
      message.success(`${data.nickname}님, 반갑습니다.`)
      router.push('/')
    },
    onError: (error: Error) => {
      message.error(error.message)
    },
  })
}
