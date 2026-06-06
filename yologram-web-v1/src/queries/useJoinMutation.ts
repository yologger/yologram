import { useMutation } from '@tanstack/react-query'
import { useNavigate } from 'react-router'
import { message } from 'antd'
import { join, type JoinRequest } from '../apis/auth'
import { getErrorMessage } from '../lib/error'

export default function useJoinMutation() {
  const navigate = useNavigate()

  return useMutation({
    mutationFn: (request: JoinRequest) => join(request),
    onSuccess: () => {
      message.success('회원가입이 완료되었습니다. 로그인해주세요.')
      navigate('/login')
    },
    onError: (error) => {
      message.error(getErrorMessage(error))
    },
  })
}
