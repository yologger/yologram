import { useMutation } from '@tanstack/react-query'
import { useNavigate } from 'react-router'
import { useSetAtom } from 'jotai'
import { App } from 'antd'
import { authAtom } from '../stores/auth'
import { login } from '../apis/auth'
import { getErrorMessage } from '../lib/error'

export default function useLoginMutation() {
  const navigate = useNavigate()
  const setAuth = useSetAtom(authAtom)

  const { message } = App.useApp()

  return useMutation({
    mutationFn: ({ email, password }: { email: string; password: string }) => login(email, password),
    onSuccess: (data) => {
      setAuth(data)
      message.success(`${data.nickname}님, 반갑습니다.`)
      navigate('/')
    },
    onError: (error) => {
      message.error(getErrorMessage(error))
    },
  })
}
