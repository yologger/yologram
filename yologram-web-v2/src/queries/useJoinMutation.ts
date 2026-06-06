'use client'

import { useMutation } from '@tanstack/react-query'
import { useRouter } from 'next/navigation'
import { message } from 'antd'
import { join, type JoinRequest } from '../apis/auth'
import { getErrorMessage } from '../lib/error'

export default function useJoinMutation() {
  const router = useRouter()

  return useMutation({
    mutationFn: (request: JoinRequest) => join(request),
    onSuccess: () => {
      message.success('회원가입이 완료되었습니다. 로그인해주세요.')
      router.push('/login')
    },
    onError: (error) => {
      message.error(getErrorMessage(error))
    },
  })
}
