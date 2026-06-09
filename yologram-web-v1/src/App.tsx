import { BrowserRouter } from 'react-router'
import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { ConfigProvider } from 'antd'
import AuthGate from './components/auth/AuthGate'
import Router from './Router'

const queryClient = new QueryClient()

export default function App() {
  return (
    <ConfigProvider theme={{ token: { colorPrimary: '#08979c' } }}>
      <QueryClientProvider client={queryClient}>
        <AuthGate>
          <BrowserRouter>
            <Router />
          </BrowserRouter>
        </AuthGate>
      </QueryClientProvider>
    </ConfigProvider>
  )
}
