'use client'

import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { ConfigProvider } from 'antd'
import { useState } from 'react'
import AuthGate from '../components/auth/AuthGate'

export default function Providers({ children }: { children: React.ReactNode }) {
  const [queryClient] = useState(() => new QueryClient())

  return (
    <ConfigProvider theme={{ token: { colorPrimary: '#1677ff' } }}>
      <QueryClientProvider client={queryClient}>
        <AuthGate>
          {children}
        </AuthGate>
      </QueryClientProvider>
    </ConfigProvider>
  )
}
