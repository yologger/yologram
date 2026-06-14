import { Tabs, Typography } from 'antd'
import { useLocation, useNavigate, Outlet } from 'react-router'
import useScrollDirection from '../../hooks/useScrollDirection'
import styles from './SubTabLayout.module.css'

interface Tab {
  key: string
  label: string
}

interface SubTabLayoutProps {
  basePath: string
  tabs: Tab[]
  title: string
  collapseOnScroll?: boolean
}

export default function SubTabLayout({ basePath, tabs, title, collapseOnScroll = false }: SubTabLayoutProps) {
  const location = useLocation()
  const navigate = useNavigate()
  const direction = useScrollDirection()

  const activeKey = tabs.find((t) => location.pathname === `${basePath}/${t.key}`)?.key ?? tabs[0].key
  const hidden = collapseOnScroll && direction === 'down'

  return (
    <div>
      <div className={`${collapseOnScroll ? styles.header : ''} ${hidden ? styles.headerHidden : ''}`}>
        <Typography.Title level={3}>{title}</Typography.Title>
        <Tabs
          activeKey={activeKey}
          onChange={(key) => navigate(`${basePath}/${key}`)}
          items={tabs.map((t) => ({ key: t.key, label: t.label }))}
        />
      </div>
      <Outlet />
    </div>
  )
}
