'use client'

import { useEffect, useRef, useState } from 'react'
import { useAtom } from 'jotai'
import { Spin } from 'antd'
import { authAtom } from '../../stores/auth'
import { validateToken } from '../../apis/auth'

export default function AuthGate({ children }: { children: React.ReactNode }) {
  const [auth, setAuth] = useAtom(authAtom)
  const [isInitialized, setIsInitialized] = useState(false)
  const validatingTokenRef = useRef<string | null>(null)

  useEffect(() => {
    if (isInitialized) return

    if (!auth?.accessToken) {
      setIsInitialized(true)
      return
    }

    if (validatingTokenRef.current === auth.accessToken) return
    validatingTokenRef.current = auth.accessToken

    validateToken()
      .then((data) => {
        setAuth({ ...auth, uid: data.uid, email: data.email, name: data.name, nickname: data.nickname })
      })
      .catch(() => {
        setAuth(null)
      })
      .finally(() => {
        setIsInitialized(true)
      })
  }, [auth, isInitialized, setAuth])

  if (!isInitialized) {
    return (
      <div style={{ display: 'flex', justifyContent: 'center', alignItems: 'center', height: '100vh' }}>
        <Spin size="large" />
      </div>
    )
  }

  return <>{children}</>
}
