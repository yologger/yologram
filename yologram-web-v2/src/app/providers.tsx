'use client'

import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { ConfigProvider, App as AntdApp } from 'antd'
import { useState } from 'react'
import AuthGate from '../components/auth/AuthGate'

export default function Providers({ children }: { children: React.ReactNode }) {
  const [queryClient] = useState(() => new QueryClient())

  return (
    <ConfigProvider theme={{ token: { colorPrimary: '#e7689a', colorLink: '#e7689a', colorLinkHover: '#ef89ad', colorLinkActive: '#d4587f' } }}>
      <AntdApp>
        <QueryClientProvider client={queryClient}>
          <AuthGate>
            {children}
          </AuthGate>
        </QueryClientProvider>
      </AntdApp>
    </ConfigProvider>
  )
}
