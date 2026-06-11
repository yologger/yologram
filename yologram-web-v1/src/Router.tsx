import { Routes, Route, Navigate } from 'react-router'
import ResponsiveLayout from './components/layout/ResponsiveLayout'
import RequireAuth from './components/auth/RequireAuth'
import LoginPage from './pages/auth/LoginPage'
import JoinPage from './pages/auth/JoinPage'
import InvestPage from './pages/invest/InvestPage'
import InvestNewsPage from './pages/invest/InvestNewsPage'
import InvestFavoriteNewsPage from './pages/invest/InvestFavoriteNewsPage'
import InvestCommunityPage from './pages/invest/InvestCommunityPage'
import InvestInfoPage from './pages/invest/InvestInfoPage'
import PoliticsPage from './pages/politics/PoliticsPage'
import PoliticsNewsPage from './pages/politics/PoliticsNewsPage'
import PoliticsFavoriteNewsPage from './pages/politics/PoliticsFavoriteNewsPage'
import PoliticsCommunityPage from './pages/politics/PoliticsCommunityPage'
import PoliticsInfoPage from './pages/politics/PoliticsInfoPage'
import TechPage from './pages/tech/TechPage'
import TechNewsPage from './pages/tech/TechNewsPage'
import TechFavoriteNewsPage from './pages/tech/TechFavoriteNewsPage'
import NotificationsPage from './pages/notifications/NotificationsPage'
import SettingsPage from './pages/settings/SettingsPage'
import ChangePasswordPage from './pages/settings/ChangePasswordPage'
import EditProfilePage from './pages/settings/EditProfilePage'

export default function Router() {
  return (
    <Routes>
      <Route path="/login" element={<LoginPage />} />
      <Route path="/join" element={<JoinPage />} />
      <Route element={<ResponsiveLayout />}>
        <Route path="/" element={<Navigate to="/invest" replace />} />
        <Route path="/invest" element={<InvestPage />}>
          <Route index element={<Navigate to="/invest/news" replace />} />
          <Route path="news" element={<InvestNewsPage />} />
          <Route path="community" element={<InvestCommunityPage />} />
          <Route path="info" element={<InvestInfoPage />} />
        </Route>
        <Route path="/politics" element={<PoliticsPage />}>
          <Route index element={<Navigate to="/politics/news" replace />} />
          <Route path="news" element={<PoliticsNewsPage />} />
          <Route path="community" element={<PoliticsCommunityPage />} />
          <Route path="info" element={<PoliticsInfoPage />} />
        </Route>
        <Route path="/tech" element={<TechPage />}>
          <Route index element={<Navigate to="/tech/news" replace />} />
          <Route path="news" element={<TechNewsPage />} />
        </Route>

        <Route element={<RequireAuth />}>
          <Route path="/invest/favorite-news" element={<InvestFavoriteNewsPage />} />
          <Route path="/politics/favorite-news" element={<PoliticsFavoriteNewsPage />} />
          <Route path="/tech/favorite-news" element={<TechFavoriteNewsPage />} />
          <Route path="/notifications" element={<NotificationsPage />} />
          <Route path="/settings" element={<SettingsPage />} />
          <Route path="/settings/change-password" element={<ChangePasswordPage />} />
          <Route path="/settings/edit-profile" element={<EditProfilePage />} />
        </Route>
      </Route>
    </Routes>
  )
}
