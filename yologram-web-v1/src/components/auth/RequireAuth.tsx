import { useEffect } from 'react'
import { Outlet, useLocation, useNavigate } from 'react-router'
import { useAtomValue } from 'jotai'
import { isAuthenticatedAtom } from '../../stores/auth'

export default function RequireAuth() {
  const isAuthenticated = useAtomValue(isAuthenticatedAtom)
  const navigate = useNavigate()
  const location = useLocation()

  useEffect(() => {
    if (!isAuthenticated) {
      // 직접 URL 진입 시에도 로그인 후 원래 목적지로 복귀할 수 있게 returnTo 전달
      navigate('/login', {
        replace: true,
        state: { returnTo: location.pathname + location.search },
      })
    }
  }, [isAuthenticated, navigate, location.pathname, location.search])

  if (!isAuthenticated) return null

  return <Outlet />
}
