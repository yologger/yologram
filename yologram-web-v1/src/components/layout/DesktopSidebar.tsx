import { useState } from 'react'
import { useLocation, useNavigate } from 'react-router'
import { Menu } from 'antd'
import { FundOutlined, GlobalOutlined, SettingOutlined, DoubleLeftOutlined, DoubleRightOutlined } from '@ant-design/icons'
import styles from './DesktopSidebar.module.css'

const menuItems = [
  { key: '/invest', label: '투자', icon: <FundOutlined /> },
  { key: '/politics', label: '정치', icon: <GlobalOutlined /> },
  { key: '/settings', label: '설정', icon: <SettingOutlined /> },
]

export default function DesktopSidebar() {
  const [collapsed, setCollapsed] = useState(false)
  const location = useLocation()
  const navigate = useNavigate()

  const selectedKey = menuItems.find((item) => location.pathname.startsWith(item.key))?.key ?? '/invest'

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
        onClick={({ key }) => navigate(key)}
      />
    </div>
  )
}
