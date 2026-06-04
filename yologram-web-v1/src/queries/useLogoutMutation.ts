import { useMutation } from '@tanstack/react-query'
import { useNavigate } from 'react-router'
import { useSetAtom } from 'jotai'
import { message } from 'antd'
import { authAtom } from '../stores/auth'
import { logout } from '../apis/auth'

export default function useLogoutMutation() {
  const navigate = useNavigate()
  const setAuth = useSetAtom(authAtom)

  return useMutation({
    mutationFn: () => logout(),
    onSuccess: () => {
      setAuth(null)
      message.success('로그아웃 되었습니다.')
      navigate('/')
    },
    onError: () => {
      setAuth(null)
      message.success('로그아웃 되었습니다.')
      navigate('/')
    },
  })
}
