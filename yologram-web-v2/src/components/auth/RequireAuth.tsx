'use client'

import { useEffect, useState } from 'react'
import { useRouter, usePathname } from 'next/navigation'
import { useAtomValue } from 'jotai'
import { isAuthenticatedAtom } from '../../stores/auth'

export default function RequireAuth({ children }: { children: React.ReactNode }) {
  const isAuthenticated = useAtomValue(isAuthenticatedAtom)
  const router = useRouter()
  const pathname = usePathname()
  const [mounted, setMounted] = useState(false)

  useEffect(() => {
    setMounted(true)
  }, [])

  useEffect(() => {
    if (mounted && !isAuthenticated) {
      // 현재 경로를 returnTo로 넘겨 로그인 성공 후 원래 화면으로 복귀
      // (오픈 리다이렉트 방지는 useLoginMutation에서 내부 경로만 허용)
      router.replace(`/login?returnTo=${encodeURIComponent(pathname)}`)
    }
  }, [mounted, isAuthenticated, router, pathname])

  if (!mounted || !isAuthenticated) return null

  return <>{children}</>
}
