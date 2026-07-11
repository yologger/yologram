import { render, type RenderOptions } from '@testing-library/react'
import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { MemoryRouter, type MemoryRouterProps } from 'react-router'
import { App as AntdApp } from 'antd'
import type { ReactElement } from 'react'

interface WrapperOptions {
  routerProps?: MemoryRouterProps
}

function createWrapper(options: WrapperOptions = {}) {
  const queryClient = new QueryClient({
    defaultOptions: {
      queries: { retry: false },
      mutations: { retry: false },
    },
  })

  return function Wrapper({ children }: { children: React.ReactNode }) {
    return (
      <QueryClientProvider client={queryClient}>
        <AntdApp>
          <MemoryRouter {...options.routerProps}>
            {children}
          </MemoryRouter>
        </AntdApp>
      </QueryClientProvider>
    )
  }
}

export function renderWithProviders(
  ui: ReactElement,
  options?: Omit<RenderOptions, 'wrapper'> & { wrapperOptions?: WrapperOptions },
) {
  const { wrapperOptions, ...renderOptions } = options ?? {}
  return render(ui, {
    wrapper: createWrapper(wrapperOptions),
    ...renderOptions,
  })
}
