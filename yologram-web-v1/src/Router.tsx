import { Routes, Route } from 'react-router'
import Home from './pages/Home'
import Test from './pages/Test'

export default function Router() {
  return (
    <Routes>
      <Route path="/" element={<Home />} />
      <Route path="/test" element={<Test />} />
    </Routes>
  )
}
