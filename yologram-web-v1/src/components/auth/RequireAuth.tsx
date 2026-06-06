import { useEffect } from 'react'
import { Outlet, useNavigate } from 'react-router'
import { useAtomValue } from 'jotai'
import { isAuthenticatedAtom } from '../../stores/auth'

export default function RequireAuth() {
  const isAuthenticated = useAtomValue(isAuthenticatedAtom)
  const navigate = useNavigate()

  useEffect(() => {
    if (!isAuthenticated) {
      navigate('/login', { replace: true })
    }
  }, [isAuthenticated, navigate])

  if (!isAuthenticated) return null

  return <Outlet />
}
