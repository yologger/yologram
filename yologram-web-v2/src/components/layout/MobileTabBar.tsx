'use client'

import { usePathname, useRouter } from 'next/navigation'
import { FundFilled, GlobalOutlined, CodeFilled, BellFilled, SettingFilled } from '@ant-design/icons'
import styles from './MobileTabBar.module.css'

const tabs = [
  { key: '/tech', label: '기술', icon: <CodeFilled /> },
  { key: '/invest', label: '투자', icon: <FundFilled /> },
  { key: '/politics', label: '정치', icon: <GlobalOutlined /> },
  { key: '/notifications', label: '알림', icon: <BellFilled /> },
  { key: '/settings', label: '설정', icon: <SettingFilled /> },
]

export default function MobileTabBar() {
  const pathname = usePathname()
  const router = useRouter()

  const activeKey = tabs.find((t) => pathname.startsWith(t.key))?.key ?? '/tech'

  return (
    <div className={styles.tabBar}>
      {tabs.map((tab) => (
        <div
          key={tab.key}
          className={`${styles.tab} ${activeKey === tab.key ? styles.active : ''}`}
          onClick={() => router.push(tab.key)}
        >
          <span className={styles.icon}>{tab.icon}</span>
          <span className={styles.label}>{tab.label}</span>
        </div>
      ))}
    </div>
  )
}
