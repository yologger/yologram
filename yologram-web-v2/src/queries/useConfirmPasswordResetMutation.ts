'use client'

import { useMutation } from '@tanstack/react-query'
import { useRouter } from 'next/navigation'
import { message } from 'antd'
import { confirmPasswordReset } from '../apis/auth'
import { getErrorMessage } from '../lib/error'

interface ConfirmVariables {
  email: string
  code: string
  newPassword: string
}

export default function useConfirmPasswordResetMutation() {
  const router = useRouter()

  return useMutation({
    mutationFn: ({ email, code, newPassword }: ConfirmVariables) =>
      confirmPasswordReset(email, code, newPassword),
    onSuccess: () => {
      message.success('비밀번호가 변경되었습니다. 새 비밀번호로 로그인해주세요.')
      router.push('/login')
    },
    onError: (error) => {
      message.error(getErrorMessage(error))
    },
  })
}
