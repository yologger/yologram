'use client'

import { useState } from 'react'
import { usePathname, useRouter } from 'next/navigation'
import { Menu } from 'antd'
import { FundOutlined, GlobalOutlined, BellOutlined, SettingOutlined, DoubleLeftOutlined, DoubleRightOutlined } from '@ant-design/icons'
import styles from './DesktopSidebar.module.css'

const menuItems = [
  { key: '/invest', label: '투자', icon: <FundOutlined /> },
  { key: '/politics', label: '정치', icon: <GlobalOutlined /> },
  { key: '/notifications', label: '알림', icon: <BellOutlined /> },
  { key: '/settings', label: '설정', icon: <SettingOutlined /> },
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
