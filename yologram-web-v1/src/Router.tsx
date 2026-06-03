import { Routes, Route, Navigate } from 'react-router'
import ResponsiveLayout from './components/layout/ResponsiveLayout'
import Invest from './pages/invest/Invest'
import InvestNews from './pages/invest/InvestNews'
import InvestCommunity from './pages/invest/InvestCommunity'
import Politics from './pages/politics/Politics'
import PoliticsNews from './pages/politics/PoliticsNews'
import PoliticsCommunity from './pages/politics/PoliticsCommunity'
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
          <Route path="community" element={<InvestCommunity />} />
        </Route>
        <Route path="/politics" element={<Politics />}>
          <Route index element={<Navigate to="/politics/news" replace />} />
          <Route path="news" element={<PoliticsNews />} />
          <Route path="community" element={<PoliticsCommunity />} />
        </Route>
        <Route path="/notifications" element={<Notifications />} />
        <Route path="/settings" element={<Settings />} />
      </Route>
    </Routes>
  )
}
