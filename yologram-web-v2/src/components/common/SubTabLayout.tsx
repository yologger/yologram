'use client'

import { Tabs } from 'antd'
import { usePathname, useRouter } from 'next/navigation'

interface Tab {
  key: string
  label: string
}

interface SubTabLayoutProps {
  basePath: string
  tabs: Tab[]
  children: React.ReactNode
}

export default function SubTabLayout({ basePath, tabs, children }: SubTabLayoutProps) {
  const pathname = usePathname()
  const router = useRouter()

  const activeKey = tabs.find((t) => pathname === `${basePath}/${t.key}`)?.key ?? tabs[0].key

  return (
    <div>
      <Tabs
        activeKey={activeKey}
        onChange={(key) => router.push(`${basePath}/${key}`)}
        items={tabs.map((t) => ({ key: t.key, label: t.label }))}
      />
      {children}
    </div>
  )
}
