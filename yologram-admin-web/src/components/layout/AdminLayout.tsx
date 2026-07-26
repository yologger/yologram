import { useState } from 'react'
import { Outlet, useLocation, useNavigate } from 'react-router'
import { Button, Drawer, Layout, Menu, Typography } from 'antd'
import { MenuOutlined } from '@ant-design/icons'
import useIsMobile from '../../hooks/useIsMobile'
import { MENU_ITEMS, findSelectedKey } from './menu'
import styles from './AdminLayout.module.css'

/**
 * 어드민 공통 레이아웃. 반응형 —
 * 데스크탑: 좌측 고정 사이드바 / 모바일: 상단 헤더의 햄버거 버튼으로 토글하는 Drawer 사이드바.
 */
export default function AdminLayout() {
  const isMobile = useIsMobile()
  const [drawerOpen, setDrawerOpen] = useState(false)
  const navigate = useNavigate()
  const location = useLocation()

  const selectedKey = findSelectedKey(location.pathname)

  if (isMobile) {
    return (
      <div className={styles.mobileLayout}>
        <header className={styles.mobileHeader}>
          <Button
            type="text"
            icon={<MenuOutlined />}
            aria-label="메뉴 열기"
            onClick={() => setDrawerOpen(true)}
          />
          <Typography.Title level={4} className={styles.logoTitle}>
            yologram admin
          </Typography.Title>
        </header>
        <Drawer
          placement="left"
          width={220}
          open={drawerOpen}
          onClose={() => setDrawerOpen(false)}
          title="yologram admin"
          styles={{ body: { padding: 0 } }}
        >
          <Menu
            mode="inline"
            items={MENU_ITEMS}
            selectedKeys={selectedKey ? [selectedKey] : []}
            onClick={({ key }) => {
              navigate(key)
              setDrawerOpen(false)
            }}
          />
        </Drawer>
        <main className={styles.mobileContent}>
          <Outlet />
        </main>
      </div>
    )
  }

  return (
    <Layout className={styles.layout}>
      <Layout.Sider theme="light" width={220} className={styles.sider}>
        <div className={styles.logo}>
          <Typography.Title level={4} className={styles.logoTitle}>
            yologram admin
          </Typography.Title>
        </div>
        <Menu
          mode="inline"
          items={MENU_ITEMS}
          selectedKeys={selectedKey ? [selectedKey] : []}
          onClick={({ key }) => navigate(key)}
        />
      </Layout.Sider>
      <Layout.Content className={styles.content}>
        <Outlet />
      </Layout.Content>
    </Layout>
  )
}
