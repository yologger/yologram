import { useMutation } from '@tanstack/react-query'
import { useLocation, useNavigate } from 'react-router'
import { useSetAtom } from 'jotai'
import { App } from 'antd'
import { authAtom } from '../stores/auth'
import { login } from '../apis/auth'
import { getErrorMessage } from '../lib/error'

export default function useLoginMutation() {
  const navigate = useNavigate()
  const location = useLocation()
  const setAuth = useSetAtom(authAtom)

  const { message } = App.useApp()

  return useMutation({
    mutationFn: ({ email, password }: { email: string; password: string }) => login(email, password),
    onSuccess: (data) => {
      setAuth(data)
      message.success(`${data.nickname}님, 반갑습니다.`)
      // 로그인 유도 모달 등에서 넘긴 returnTo가 있으면 원래 화면으로 복귀, 없으면 기존대로 홈으로
      const returnTo = (location.state as { returnTo?: string } | null)?.returnTo
      navigate(returnTo ?? '/')
    },
    onError: (error) => {
      message.error(getErrorMessage(error))
    },
  })
}
