import { Tabs, Typography } from 'antd'
import { useLocation, useNavigate, Outlet } from 'react-router'
import useScrollDirection from '../../hooks/useScrollDirection'
import useIsMobile from '../../hooks/useIsMobile'
import SearchBar from './SearchBar'
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
  const isMobile = useIsMobile()

  const activeKey = tabs.find((t) => location.pathname === `${basePath}/${t.key}`)?.key ?? tabs[0].key
  const hidden = collapseOnScroll && direction === 'down'

  return (
    <div>
      <div className={`${collapseOnScroll ? styles.header : ''} ${hidden ? styles.headerHidden : ''}`}>
        <div className={styles.titleRow}>
          <Typography.Title level={3}>{title}</Typography.Title>
          {/* 모바일: 타이틀 행 오른쪽에 돋보기 아이콘 (탭하면 검색 오버레이) */}
          {isMobile && <SearchBar basePath={basePath} />}
        </div>
        {/* 데스크탑: 타이틀과 탭 사이 인라인 검색바 */}
        {!isMobile && <SearchBar basePath={basePath} />}
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
