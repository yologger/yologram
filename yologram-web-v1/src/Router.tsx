import { Routes, Route, Navigate } from 'react-router'
import ResponsiveLayout from './components/layout/ResponsiveLayout'
import Invest from './pages/invest/Invest'
import InvestNews from './pages/invest/InvestNews'
import InvestFavoriteNews from './pages/invest/InvestFavoriteNews'
import InvestCommunity from './pages/invest/InvestCommunity'
import InvestInfo from './pages/invest/InvestInfo'
import Politics from './pages/politics/Politics'
import PoliticsNews from './pages/politics/PoliticsNews'
import PoliticsFavoriteNews from './pages/politics/PoliticsFavoriteNews'
import PoliticsCommunity from './pages/politics/PoliticsCommunity'
import PoliticsInfo from './pages/politics/PoliticsInfo'
import Tech from './pages/tech/Tech'
import TechNews from './pages/tech/TechNews'
import TechFavoriteNews from './pages/tech/TechFavoriteNews'
import Notifications from './pages/notifications/Notifications'
import Settings from './pages/settings/Settings'

export default function Router() {
  return (
    <Routes>
      <Route element={<ResponsiveLayout />}>
        <Route path="/" element={<Navigate to="/invest" replace />} />
        <Route path="/invest" element={<Invest />}>
          <Route index element={<Navigate to="/invest/news" replace />} />
          <Route path="news" element={<InvestNews />} />
          <Route path="favorite-news" element={<InvestFavoriteNews />} />
          <Route path="community" element={<InvestCommunity />} />
          <Route path="info" element={<InvestInfo />} />
        </Route>
        <Route path="/politics" element={<Politics />}>
          <Route index element={<Navigate to="/politics/news" replace />} />
          <Route path="news" element={<PoliticsNews />} />
          <Route path="favorite-news" element={<PoliticsFavoriteNews />} />
          <Route path="community" element={<PoliticsCommunity />} />
          <Route path="info" element={<PoliticsInfo />} />
        </Route>
        <Route path="/tech" element={<Tech />}>
          <Route index element={<Navigate to="/tech/news" replace />} />
          <Route path="news" element={<TechNews />} />
          <Route path="favorite-news" element={<TechFavoriteNews />} />
        </Route>
        <Route path="/notifications" element={<Notifications />} />
        <Route path="/settings" element={<Settings />} />
      </Route>
    </Routes>
  )
}
