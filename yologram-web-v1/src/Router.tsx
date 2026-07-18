import { Routes, Route, Navigate } from 'react-router'
import ResponsiveLayout from './components/layout/ResponsiveLayout'
import RequireAuth from './components/auth/RequireAuth'
import LoginPage from './pages/auth/LoginPage'
import JoinPage from './pages/auth/JoinPage'
import ForgotPasswordPage from './pages/auth/ForgotPasswordPage'
import ComingSoon from './components/common/ComingSoon'
// TODO(invest/politics): 섹션 구현 시작 시 아래 import 주석 해제 + ComingSoon 라우트 제거
// import InvestPage from './pages/invest/InvestPage'
// import InvestArticlesPage from './pages/invest/InvestArticlesPage'
// import InvestFavoriteArticlesPage from './pages/invest/InvestFavoriteArticlesPage'
// import InvestCommunityPage from './pages/invest/InvestCommunityPage'
// import InvestInfoPage from './pages/invest/InvestInfoPage'
// import PoliticsPage from './pages/politics/PoliticsPage'
// import PoliticsArticlesPage from './pages/politics/PoliticsArticlesPage'
// import PoliticsFavoriteArticlesPage from './pages/politics/PoliticsFavoriteArticlesPage'
// import PoliticsCommunityPage from './pages/politics/PoliticsCommunityPage'
// import PoliticsInfoPage from './pages/politics/PoliticsInfoPage'
import TechPage from './pages/tech/TechPage'
import TechArticlesPage from './pages/tech/TechArticlesPage'
import TechFavoriteArticlesPage from './pages/tech/TechFavoriteArticlesPage'
import TechCommunityPage from './pages/tech/TechCommunityPage'
// TODO(tech/jobs): 채용 구현 시작 시 주석 해제 + ComingSoon 라우트 제거
// import TechJobsPage from './pages/tech/TechJobsPage'
import CommunityWritePage from './pages/tech/community/CommunityWritePage'
import CommunityDetailPage from './pages/tech/community/CommunityDetailPage'
// TODO(notifications): 알림 구현 시작 시 주석 해제 + ComingSoon 라우트 제거
// import NotificationsPage from './pages/notifications/NotificationsPage'
import SettingsPage from './pages/settings/SettingsPage'
import ChangePasswordPage from './pages/settings/ChangePasswordPage'
import EditProfilePage from './pages/settings/EditProfilePage'
import MyPostsPage from './pages/settings/MyPostsPage'

export default function Router() {
  return (
    <Routes>
      <Route path="/login" element={<LoginPage />} />
      <Route path="/join" element={<JoinPage />} />
      <Route path="/forgot-password" element={<ForgotPasswordPage />} />
      <Route element={<RequireAuth />}>
        <Route path="/tech/community/write" element={<CommunityWritePage />} />
        <Route path="/tech/community/:postId/edit" element={<CommunityWritePage />} />
      </Route>
      <Route path="/tech/community/:postId" element={<CommunityDetailPage />} />
      <Route element={<ResponsiveLayout />}>
        <Route path="/" element={<Navigate to="/tech" replace />} />
        {/* invest/politics 준비 중: 구현 시작 시 ComingSoon 라우트 제거하고 아래 주석 블록 복구 */}
        <Route path="/invest/*" element={<ComingSoon title="투자" />} />
        <Route path="/politics/*" element={<ComingSoon title="정치" />} />
        {/*
        <Route path="/invest" element={<InvestPage />}>
          <Route index element={<Navigate to="/invest/articles" replace />} />
          <Route path="articles" element={<InvestArticlesPage />} />
          <Route path="community" element={<InvestCommunityPage />} />
          <Route path="info" element={<InvestInfoPage />} />
          <Route element={<RequireAuth />}>
            <Route path="favorite-articles" element={<InvestFavoriteArticlesPage />} />
          </Route>
        </Route>
        <Route path="/politics" element={<PoliticsPage />}>
          <Route index element={<Navigate to="/politics/articles" replace />} />
          <Route path="articles" element={<PoliticsArticlesPage />} />
          <Route path="community" element={<PoliticsCommunityPage />} />
          <Route path="info" element={<PoliticsInfoPage />} />
          <Route element={<RequireAuth />}>
            <Route path="favorite-articles" element={<PoliticsFavoriteArticlesPage />} />
          </Route>
        </Route>
        */}
        <Route path="/tech" element={<TechPage />}>
          <Route index element={<Navigate to="/tech/articles" replace />} />
          <Route path="articles" element={<TechArticlesPage />} />
          <Route path="community" element={<TechCommunityPage />} />
          {/* 채용 준비 중: 구현 시작 시 ComingSoon 제거하고 아래 주석 복구 */}
          <Route path="jobs" element={<ComingSoon />} />
          {/* <Route path="jobs" element={<TechJobsPage />} /> */}
          <Route element={<RequireAuth />}>
            <Route path="favorite-articles" element={<TechFavoriteArticlesPage />} />
          </Route>
        </Route>

        {/* 알림 준비 중: 구현 시작 시 ComingSoon 제거하고 아래 RequireAuth 내 NotificationsPage 복구 */}
        <Route path="/notifications" element={<ComingSoon title="알림" />} />
        <Route element={<RequireAuth />}>
          {/* <Route path="/notifications" element={<NotificationsPage />} /> */}
          <Route path="/settings" element={<SettingsPage />} />
          <Route path="/settings/change-password" element={<ChangePasswordPage />} />
          <Route path="/settings/edit-profile" element={<EditProfilePage />} />
          <Route path="/settings/my-posts" element={<MyPostsPage />} />
        </Route>
      </Route>
    </Routes>
  )
}
