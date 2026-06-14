import { BrowserRouter } from 'react-router'
import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { ConfigProvider, App as AntdApp } from 'antd'
import AuthGate from './components/auth/AuthGate'
import Router from './Router'

const queryClient = new QueryClient()

export default function App() {
  return (
    <ConfigProvider theme={{ token: { colorPrimary: '#08979c', colorLink: '#08979c', colorLinkHover: '#2aa7ac', colorLinkActive: '#066b6f' } }}>
      <AntdApp>
        <QueryClientProvider client={queryClient}>
          <AuthGate>
            <BrowserRouter>
              <Router />
            </BrowserRouter>
          </AuthGate>
        </QueryClientProvider>
      </AntdApp>
    </ConfigProvider>
  )
}
