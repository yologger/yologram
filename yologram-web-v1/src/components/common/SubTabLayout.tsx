import { Tabs } from 'antd'
import { useLocation, useNavigate, Outlet } from 'react-router'

interface Tab {
  key: string
  label: string
}

interface SubTabLayoutProps {
  basePath: string
  tabs: Tab[]
}

export default function SubTabLayout({ basePath, tabs }: SubTabLayoutProps) {
  const location = useLocation()
  const navigate = useNavigate()

  const activeKey = tabs.find((t) => location.pathname === `${basePath}/${t.key}`)?.key ?? tabs[0].key

  return (
    <div>
      <Tabs
        activeKey={activeKey}
        onChange={(key) => navigate(`${basePath}/${key}`)}
        items={tabs.map((t) => ({ key: t.key, label: t.label }))}
      />
      <Outlet />
    </div>
  )
}
