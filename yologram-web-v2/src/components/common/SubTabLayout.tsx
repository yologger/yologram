'use client'

import { Tabs, Typography } from 'antd'
import { usePathname, useRouter } from 'next/navigation'
import useScrollDirection from '@/hooks/useScrollDirection'
import styles from './SubTabLayout.module.css'

interface Tab {
  key: string
  label: string
}

interface SubTabLayoutProps {
  basePath: string
  tabs: Tab[]
  title: string
  children: React.ReactNode
  collapseOnScroll?: boolean
}

export default function SubTabLayout({ basePath, tabs, title, children, collapseOnScroll = false }: SubTabLayoutProps) {
  const pathname = usePathname()
  const router = useRouter()
  const direction = useScrollDirection()

  const activeKey = tabs.find((t) => pathname === `${basePath}/${t.key}`)?.key ?? tabs[0].key
  const hidden = collapseOnScroll && direction === 'down'

  return (
    <div>
      <div className={`${collapseOnScroll ? styles.header : ''} ${hidden ? styles.headerHidden : ''}`}>
        <Typography.Title level={3}>{title}</Typography.Title>
        <Tabs
          activeKey={activeKey}
          onChange={(key) => router.push(`${basePath}/${key}`)}
          items={tabs.map((t) => ({ key: t.key, label: t.label }))}
        />
      </div>
      {children}
    </div>
  )
}
