import { useMutation } from '@tanstack/react-query'
import { message } from 'antd'
import { verifyEmail } from '../apis/auth'
import { getErrorMessage } from '../lib/error'

interface VerifyEmailVariables {
  email: string
  code: string
}

export default function useVerifyEmailMutation() {
  return useMutation({
    mutationFn: ({ email, code }: VerifyEmailVariables) => verifyEmail(email, code),
    onSuccess: () => {
      message.success('이메일 인증이 완료되었습니다.')
    },
    onError: (error) => {
      message.error(getErrorMessage(error))
    },
  })
}
