'use client'

import { useState } from 'react'
import { usePathname, useRouter } from 'next/navigation'
import { Menu } from 'antd'
import { FundFilled, GlobalOutlined, CodeFilled, BellFilled, SettingFilled, DoubleLeftOutlined, DoubleRightOutlined } from '@ant-design/icons'
import styles from './DesktopSidebar.module.css'

const menuItems = [
  { key: '/invest', label: '투자', icon: <FundFilled /> },
  { key: '/politics', label: '정치', icon: <GlobalOutlined /> },
  { key: '/tech', label: '기술', icon: <CodeFilled /> },
  { key: '/notifications', label: '알림', icon: <BellFilled /> },
  { key: '/settings', label: '설정', icon: <SettingFilled /> },
]

export default function DesktopSidebar() {
  const [collapsed, setCollapsed] = useState(false)
  const pathname = usePathname()
  const router = useRouter()

  const selectedKey = menuItems.find((item) => pathname.startsWith(item.key))?.key ?? '/invest'

  return (
    <div className={`${styles.sidebar} ${collapsed ? styles.collapsed : ''}`}>
      <div className={styles.toggle} onClick={() => setCollapsed(!collapsed)}>
        {collapsed ? <DoubleRightOutlined /> : <DoubleLeftOutlined />}
      </div>
      <Menu
        mode="inline"
        selectedKeys={[selectedKey]}
        inlineCollapsed={collapsed}
        items={menuItems}
        onClick={({ key }) => router.push(key)}
      />
    </div>
  )
}
