import { BrowserRouter } from 'react-router'
import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { ConfigProvider, App as AntdApp } from 'antd'
import Router from './Router'

const queryClient = new QueryClient()

export default function App() {
  return (
    <ConfigProvider theme={{ token: { colorPrimary: '#1677ff', colorLink: '#1677ff', colorLinkHover: '#4096ff', colorLinkActive: '#0958d9' } }}>
      <AntdApp>
        <QueryClientProvider client={queryClient}>
          <BrowserRouter>
            <Router />
          </BrowserRouter>
        </QueryClientProvider>
      </AntdApp>
    </ConfigProvider>
  )
}
