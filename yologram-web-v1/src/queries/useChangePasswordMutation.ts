import { useMutation } from '@tanstack/react-query'
import { useNavigate } from 'react-router'
import { App } from 'antd'
import { changePassword, type ChangePasswordRequest } from '../apis/auth'
import { getErrorMessage } from '../lib/error'

export default function useChangePasswordMutation() {
  const navigate = useNavigate()

  const { message } = App.useApp()

  return useMutation({
    mutationFn: (request: ChangePasswordRequest) => changePassword(request),
    onSuccess: () => {
      message.success('비밀번호가 변경되었습니다.')
      navigate('/settings')
    },
    onError: (error) => {
      message.error(getErrorMessage(error))
    },
  })
}
