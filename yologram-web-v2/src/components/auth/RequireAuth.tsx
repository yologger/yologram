'use client'

import { useEffect, useState } from 'react'
import { useRouter } from 'next/navigation'
import { useAtomValue } from 'jotai'
import { isAuthenticatedAtom } from '../../stores/auth'

export default function RequireAuth({ children }: { children: React.ReactNode }) {
  const isAuthenticated = useAtomValue(isAuthenticatedAtom)
  const router = useRouter()
  const [mounted, setMounted] = useState(false)

  useEffect(() => {
    setMounted(true)
  }, [])

  useEffect(() => {
    if (mounted && !isAuthenticated) {
      router.replace('/login')
    }
  }, [mounted, isAuthenticated, router])

  if (!mounted || !isAuthenticated) return null

  return <>{children}</>
}
