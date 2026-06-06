import { useEffect, useRef, useState, type ReactNode } from 'react'
import { Spin } from 'antd'
import { useAtom } from 'jotai'
import { validateToken } from '../../apis/auth'
import { authAtom } from '../../stores/auth'
import styles from './AuthGate.module.css'

interface AuthGateProps {
  children: ReactNode
}

export default function AuthGate({ children }: AuthGateProps) {
  const [auth, setAuth] = useAtom(authAtom)
  const [isInitialized, setIsInitialized] = useState(false)
  const validatingTokenRef = useRef<string | null>(null)

  useEffect(() => {
    const accessToken = auth?.accessToken

    if (!accessToken) {
      validatingTokenRef.current = null
      setIsInitialized(true)
      return
    }

    if (isInitialized && validatingTokenRef.current === accessToken) {
      return
    }

    let isActive = true
    validatingTokenRef.current = accessToken
    setIsInitialized(false)

    validateToken()
      .then((user) => {
        if (!isActive) return
        setAuth({
          ...user,
          accessToken,
        })
      })
      .catch(() => {
        if (!isActive) return
        setAuth(null)
      })
      .finally(() => {
        if (!isActive) return
        setIsInitialized(true)
      })

    return () => {
      isActive = false
    }
  }, [auth?.accessToken, isInitialized, setAuth])

  if (!isInitialized) {
    return (
      <div className={styles.container} role="status" aria-label="인증 확인 중">
        <Spin size="large" />
      </div>
    )
  }

  return <>{children}</>
}
