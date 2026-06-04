'use client'

import { useEffect } from 'react'
import { useRouter } from 'next/navigation'
import { useAtomValue } from 'jotai'
import { isAuthenticatedAtom } from '../../stores/auth'

export default function RequireAuth({ children }: { children: React.ReactNode }) {
  const isAuthenticated = useAtomValue(isAuthenticatedAtom)
  const router = useRouter()

  useEffect(() => {
    if (!isAuthenticated) {
      router.replace('/login')
    }
  }, [isAuthenticated, router])

  if (!isAuthenticated) return null

  return <>{children}</>
}
