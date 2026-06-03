import { useLocation, useNavigate } from 'react-router'
import { FundFilled, GlobalOutlined, CodeFilled, BellFilled, SettingFilled } from '@ant-design/icons'
import styles from './MobileTabBar.module.css'

const tabs = [
  { key: '/invest', label: '투자', icon: <FundFilled /> },
  { key: '/politics', label: '정치', icon: <GlobalOutlined /> },
  { key: '/tech', label: '기술', icon: <CodeFilled /> },
  { key: '/notifications', label: '알림', icon: <BellFilled /> },
  { key: '/settings', label: '설정', icon: <SettingFilled /> },
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
