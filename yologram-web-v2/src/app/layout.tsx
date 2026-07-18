import type { Metadata } from 'next'
import '@/styles/global.css'
import { AntdRegistry } from '@ant-design/nextjs-registry'
import Providers from './providers'

export const metadata: Metadata = {
  title: 'yologram (v2)',
}

// 모든 페이지를 감싸는 최상위 레이아웃 (서버 컴포넌트)
// children: Next.js가 URL에 맞는 페이지를 자동으로 주입
// 예: /invest/articles 접근 시 → (main)/layout → invest/layout → invest/articles/page 순으로 중첩
export default function RootLayout({
  children,
}: {
  children: React.ReactNode
}) {
  return (
    <html lang="ko">
      <body>
        {/* AntdRegistry: Ant Design CSS-in-JS 스타일을 SSR 시 서버에서 미리 수집 */}
        <AntdRegistry>
          {/* Providers: 클라이언트 컴포넌트 (QueryClientProvider 등) */}
          <Providers>{children}</Providers>
        </AntdRegistry>
      </body>
    </html>
  )
}
