'use client'

import { useMutation } from '@tanstack/react-query'
import { useRouter } from 'next/navigation'
import { useSetAtom } from 'jotai'
import { App } from 'antd'
import { authAtom } from '../stores/auth'
import { login } from '../apis/auth'
import { getErrorMessage } from '../lib/error'

export default function useLoginMutation() {
  const router = useRouter()
  const setAuth = useSetAtom(authAtom)

  const { message } = App.useApp()

  return useMutation({
    mutationFn: ({ email, password }: { email: string; password: string }) => login(email, password),
    onSuccess: (data) => {
      setAuth(data)
      message.success(`${data.nickname}님, 반갑습니다.`)
      // returnTo가 있으면 로그인 유도 모달을 거쳐 온 경우 — 원래 화면으로 복귀.
      // useSearchParams는 페이지 프리렌더 시 Suspense 경계를 요구하므로 클릭 시점의 location에서 직접 읽는다.
      const returnTo = new URLSearchParams(window.location.search).get('returnTo')
      // 오픈 리다이렉트 방지 — 내부 경로('/시작, '//' 제외)만 허용, 그 외에는 기존 동작(메인 이동) 유지
      const isInternalPath = !!returnTo && returnTo.startsWith('/') && !returnTo.startsWith('//')
      router.push(isInternalPath ? returnTo : '/')
    },
    onError: (error) => {
      message.error(getErrorMessage(error))
    },
  })
}
