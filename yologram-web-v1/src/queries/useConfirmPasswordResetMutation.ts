import { useMutation } from '@tanstack/react-query'
import { useNavigate } from 'react-router'
import { App } from 'antd'
import { confirmPasswordReset } from '../apis/auth'
import { getErrorMessage } from '../lib/error'

interface ConfirmVariables {
  email: string
  code: string
  newPassword: string
}

export default function useConfirmPasswordResetMutation() {
  const navigate = useNavigate()

  const { message } = App.useApp()

  return useMutation({
    mutationFn: ({ email, code, newPassword }: ConfirmVariables) =>
      confirmPasswordReset(email, code, newPassword),
    onSuccess: () => {
      message.success('비밀번호가 변경되었습니다. 새 비밀번호로 로그인해주세요.')
      navigate('/login')
    },
    onError: (error) => {
      message.error(getErrorMessage(error))
    },
  })
}
