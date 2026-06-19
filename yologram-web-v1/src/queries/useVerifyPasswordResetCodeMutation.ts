import { useMutation } from '@tanstack/react-query'
import { App } from 'antd'
import { verifyPasswordResetCode } from '../apis/auth'
import { getErrorMessage } from '../lib/error'

interface VerifyVariables {
  email: string
  code: string
}

export default function useVerifyPasswordResetCodeMutation() {
  const { message } = App.useApp()

  return useMutation({
    mutationFn: ({ email, code }: VerifyVariables) => verifyPasswordResetCode(email, code),
    onSuccess: () => {
      message.success('코드가 확인되었습니다. 새 비밀번호를 입력해주세요.')
    },
    onError: (error) => {
      message.error(getErrorMessage(error))
    },
  })
}
