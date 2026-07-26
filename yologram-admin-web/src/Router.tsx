import { Routes, Route, Navigate } from 'react-router'
import AdminLayout from './components/layout/AdminLayout'
import RequireAuth from './components/auth/RequireAuth'
import LoginPage from './pages/auth/LoginPage'
import UmsPage from './pages/ums/UmsPage'
import ComingSoon from './components/common/ComingSoon'

/**
 * 어드민 라우팅. 어드민은 전 메뉴가 인증 필요 — /login 외 전체를 RequireAuth로 보호한다.
 * 각 메뉴 기능을 구현하면 ComingSoon 매핑을 해당 페이지 컴포넌트로 교체한다.
 */
export default function Router() {
  return (
    <Routes>
      <Route path="/login" element={<LoginPage />} />
      <Route element={<RequireAuth />}>
        <Route element={<AdminLayout />}>
          <Route path="/" element={<Navigate to="/dashboard" replace />} />
          <Route path="/dashboard" element={<ComingSoon title="대시보드" />} />
          <Route path="/ums" element={<UmsPage />}>
            <Route index element={<Navigate to="/ums/users" replace />} />
            <Route path="users" element={<ComingSoon />} />
            <Route path="admin-users" element={<ComingSoon />} />
          </Route>
          <Route path="/categories" element={<ComingSoon title="카테고리 관리" />} />
          <Route path="/posts" element={<ComingSoon title="게시글 관리" />} />
          <Route path="/feeds" element={<ComingSoon title="RSS 피드 관리" />} />
          <Route path="*" element={<Navigate to="/dashboard" replace />} />
        </Route>
      </Route>
    </Routes>
  )
}
