import { Routes, Route, Navigate } from 'react-router'
import AdminLayout from './components/layout/AdminLayout'
import RequireAuth from './components/auth/RequireAuth'
import LoginPage from './pages/auth/LoginPage'
import NewsSourcesPage from './pages/news/NewsSourcesPage'
import ComingSoon from './components/common/ComingSoon'

/**
 * 어드민 라우팅. 어드민은 전 메뉴가 인증 필요 — /login 외 전체를 RequireAuth로 보호한다.
 * 하위 분류 이동은 AdminLayout 사이드바가 담당하며, 각 화면은 기능 구현 시 ComingSoon을 페이지 컴포넌트로 교체한다.
 */
export default function Router() {
  return (
    <Routes>
      <Route path="/login" element={<LoginPage />} />
      <Route element={<RequireAuth />}>
        <Route element={<AdminLayout />}>
          <Route path="/" element={<Navigate to="/notices" replace />} />
          <Route path="/notices" element={<ComingSoon title="공지 관리" />} />
          <Route path="/ums" element={<Navigate to="/ums/users" replace />} />
          <Route path="/ums/users" element={<ComingSoon title="유저 관리" />} />
          <Route path="/ums/admin-users" element={<ComingSoon title="어드민 관리" />} />
          <Route path="/categories" element={<ComingSoon title="카테고리 관리" />} />
          <Route path="/posts" element={<ComingSoon title="게시글 관리" />} />
          <Route path="/news" element={<Navigate to="/news/tech" replace />} />
          <Route path="/news/tech" element={<ComingSoon title="기술 뉴스 관리" />} />
          <Route path="/news/invest" element={<ComingSoon title="투자 뉴스 관리" />} />
          <Route path="/news/politics" element={<ComingSoon title="정치 뉴스 관리" />} />
          <Route path="/news/tech/sources" element={<NewsSourcesPage />} />
          <Route path="*" element={<Navigate to="/notices" replace />} />
        </Route>
      </Route>
    </Routes>
  )
}
