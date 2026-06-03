import { Routes, Route, Navigate } from 'react-router'
import ResponsiveLayout from './components/layout/ResponsiveLayout'
import Invest from './pages/invest/Invest'
import Politics from './pages/politics/Politics'
import Notifications from './pages/notifications/Notifications'
import Settings from './pages/settings/Settings'

export default function Router() {
  return (
    <Routes>
      <Route element={<ResponsiveLayout />}>
        <Route path="/" element={<Navigate to="/invest" replace />} />
        <Route path="/invest" element={<Invest />} />
        <Route path="/politics" element={<Politics />} />
        <Route path="/notifications" element={<Notifications />} />
        <Route path="/settings" element={<Settings />} />
      </Route>
    </Routes>
  )
}
