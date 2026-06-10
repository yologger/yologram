import { useMutation } from '@tanstack/react-query'
import { useNavigate } from 'react-router'
import { message } from 'antd'
import { changePassword, type ChangePasswordRequest } from '../apis/auth'
import { getErrorMessage } from '../lib/error'

export default function useChangePasswordMutation() {
  const navigate = useNavigate()

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
