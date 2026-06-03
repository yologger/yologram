import { useLocation, useNavigate } from 'react-router'
import { FundOutlined, GlobalOutlined, BellOutlined, SettingOutlined } from '@ant-design/icons'
import styles from './MobileTabBar.module.css'

const tabs = [
  { key: '/invest', label: '투자', icon: <FundOutlined /> },
  { key: '/politics', label: '정치', icon: <GlobalOutlined /> },
  { key: '/notifications', label: '알림', icon: <BellOutlined /> },
  { key: '/settings', label: '설정', icon: <SettingOutlined /> },
]

export default function MobileTabBar() {
  const location = useLocation()
  const navigate = useNavigate()

  const activeKey = tabs.find((t) => location.pathname.startsWith(t.key))?.key ?? '/invest'

  return (
    <div className={styles.tabBar}>
      {tabs.map((tab) => (
        <div
          key={tab.key}
          className={`${styles.tab} ${activeKey === tab.key ? styles.active : ''}`}
          onClick={() => navigate(tab.key)}
        >
          <span className={styles.icon}>{tab.icon}</span>
          <span className={styles.label}>{tab.label}</span>
        </div>
      ))}
    </div>
  )
}
