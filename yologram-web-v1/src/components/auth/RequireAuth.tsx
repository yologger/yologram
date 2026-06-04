import { Navigate, Outlet } from 'react-router'
import { useAtomValue } from 'jotai'
import { isAuthenticatedAtom } from '../../stores/auth'

export default function RequireAuth() {
  const isAuthenticated = useAtomValue(isAuthenticatedAtom)

  if (!isAuthenticated) {
    return <Navigate to="/login" replace />
  }

  return <Outlet />
}
