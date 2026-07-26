import { Tabs, Typography } from 'antd'
import { useLocation, useNavigate, Outlet } from 'react-router'

interface Tab {
  key: string
  label: string
}

interface SubTabLayoutProps {
  basePath: string
  tabs: Tab[]
  title: string
}

/** 섹션 내 서브탭 공통 레이아웃 (web-v1 SubTabLayout 미러 — 어드민은 스크롤 접힘 미사용). */
export default function SubTabLayout({ basePath, tabs, title }: SubTabLayoutProps) {
  const location = useLocation()
  const navigate = useNavigate()

  const activeKey = tabs.find((t) => location.pathname === `${basePath}/${t.key}`)?.key ?? tabs[0].key

  return (
    <div>
      <Typography.Title level={3}>{title}</Typography.Title>
      <Tabs
        activeKey={activeKey}
        onChange={(key) => navigate(`${basePath}/${key}`)}
        items={tabs.map((t) => ({ key: t.key, label: t.label }))}
      />
      <Outlet />
    </div>
  )
}
