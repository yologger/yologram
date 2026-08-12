'use client'

import { useCallback } from 'react'
import { useRouter, usePathname } from 'next/navigation'
import { useAtomValue } from 'jotai'
import { App } from 'antd'
import { authAtom } from '@/stores/auth'

// 인증이 필요한 액션(좋아요·댓글 등)의 공용 가드.
// 로그인 상태면 true를 반환하고, 비로그인이면 로그인 유도 모달을 띄운 뒤 false를 반환한다.
// 모달에서 로그인을 선택하면 현재 경로를 returnTo 쿼리로 넘겨 로그인 성공 후 복귀할 수 있게 한다.
export default function useRequireAuth() {
  const auth = useAtomValue(authAtom)
  const router = useRouter()
  const pathname = usePathname()
  const { modal } = App.useApp()

  return useCallback(() => {
    if (auth) return true

    modal.confirm({
      title: '로그인이 필요해요',
      content: '좋아요와 댓글은 로그인 후 이용할 수 있어요.',
      okText: '로그인',
      cancelText: '취소',
      onOk: () => {
        // App Router에는 navigation state가 없으므로 쿼리 파라미터로 복귀 경로 전달
        router.push(`/login?returnTo=${encodeURIComponent(pathname)}`)
      },
    })
    return false
  }, [auth, modal, router, pathname])
}
