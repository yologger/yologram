import { Routes, Route, Navigate } from 'react-router'
import AdminLayout from './components/layout/AdminLayout'
import ComingSoon from './components/common/ComingSoon'

/**
 * 전 메뉴 준비 중 상태의 부트스트랩 라우팅.
 * 각 메뉴 기능을 구현하면 ComingSoon 매핑을 해당 페이지 컴포넌트로 교체한다.
 * 어드민 인증(로그인·RequireAuth)은 인증 방식 결정 후 추가 예정.
 */
export default function Router() {
  return (
    <Routes>
      <Route element={<AdminLayout />}>
        <Route path="/" element={<Navigate to="/dashboard" replace />} />
        <Route path="/dashboard" element={<ComingSoon title="대시보드" />} />
        <Route path="/users" element={<ComingSoon title="회원 관리" />} />
        <Route path="/categories" element={<ComingSoon title="카테고리 관리" />} />
        <Route path="/posts" element={<ComingSoon title="게시글 관리" />} />
        <Route path="/feeds" element={<ComingSoon title="RSS 피드 관리" />} />
        <Route path="*" element={<Navigate to="/dashboard" replace />} />
      </Route>
    </Routes>
  )
}
