import { useMutation } from '@tanstack/react-query'
import { App } from 'antd'
import { withdraw } from '../apis/auth'
import { getErrorMessage } from '../lib/error'

export default function useWithdrawMutation() {
  const { message } = App.useApp()

  return useMutation({
    mutationFn: () => withdraw(),
    onSuccess: () => {
      message.success('회원탈퇴가 완료되었습니다.')
      localStorage.removeItem('auth')
      window.location.href = '/login'
    },
    onError: (error) => {
      message.error(getErrorMessage(error))
    },
  })
}
