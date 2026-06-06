import { BrowserRouter } from 'react-router'
import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import AuthGate from './components/auth/AuthGate'
import Router from './Router'

const queryClient = new QueryClient()

export default function App() {
  return (
    <QueryClientProvider client={queryClient}>
      <AuthGate>
        <BrowserRouter>
          <Router />
        </BrowserRouter>
      </AuthGate>
    </QueryClientProvider>
  )
}
