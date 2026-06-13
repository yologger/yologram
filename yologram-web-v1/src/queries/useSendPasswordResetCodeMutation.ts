import { useMutation } from '@tanstack/react-query'
import { message } from 'antd'
import { sendPasswordResetCode } from '../apis/auth'
import { getErrorMessage } from '../lib/error'

export default function useSendPasswordResetCodeMutation() {
  return useMutation({
    mutationFn: (email: string) => sendPasswordResetCode(email),
    onSuccess: () => {
      message.success('재설정 코드를 발송했습니다. 메일을 확인해주세요.')
    },
    onError: (error) => {
      message.error(getErrorMessage(error))
    },
  })
}
