'use client'

import { useMutation } from '@tanstack/react-query'
import { useRouter } from 'next/navigation'
import { message } from 'antd'
import { join, type JoinRequest } from '../apis/auth'
import axios from 'axios'

export default function useJoinMutation() {
  const router = useRouter()

  return useMutation({
    mutationFn: (request: JoinRequest) => join(request),
    onSuccess: () => {
      message.success('회원가입이 완료되었습니다. 로그인해주세요.')
      router.push('/login')
    },
    onError: (error) => {
      if (axios.isAxiosError(error) && error.response?.data?.errorMessage) {
        message.error(error.response.data.errorMessage)
      } else {
        message.error('회원가입에 실패했습니다.')
      }
    },
  })
}
