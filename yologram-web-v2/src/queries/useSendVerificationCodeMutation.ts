'use client'

import { useMutation } from '@tanstack/react-query'
import { App } from 'antd'
import { sendVerificationCode } from '../apis/auth'
import { getErrorMessage } from '../lib/error'

export default function useSendVerificationCodeMutation() {
  const { message } = App.useApp()

  return useMutation({
    mutationFn: (email: string) => sendVerificationCode(email),
    onSuccess: () => {
      message.success('인증 코드를 발송했습니다. 메일을 확인해주세요.')
    },
    onError: (error) => {
      message.error(getErrorMessage(error))
    },
  })
}
