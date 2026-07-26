import { useState } from 'react'
import { Outlet, useLocation, useNavigate } from 'react-router'
import { App, Button, Drawer, Layout, Menu, Typography } from 'antd'
import { LogoutOutlined, MenuOutlined, UserOutlined } from '@ant-design/icons'
import { useAtomValue } from 'jotai'
import { authAtom } from '../../stores/auth'
import useLogoutMutation from '../../queries/useLogoutMutation'
import useIsMobile from '../../hooks/useIsMobile'
import { MENU_ITEMS, findSelectedKey } from './menu'
import styles from './AdminLayout.module.css'

/**
 * 어드민 공통 레이아웃. 반응형 —
 * 데스크탑: 좌측 고정 사이드바 / 모바일: 상단 헤더의 햄버거 버튼으로 토글하는 Drawer 사이드바.
 * 사이드바(데스크탑)·Drawer(모바일) 하단에 로그인된 어드민 이름과 로그아웃 버튼을 표시한다.
 */
export default function AdminLayout() {
  const isMobile = useIsMobile()
  const [drawerOpen, setDrawerOpen] = useState(false)
  const navigate = useNavigate()
  const location = useLocation()
  const auth = useAtomValue(authAtom)
  const { modal } = App.useApp()
  const { mutate: logoutMutate } = useLogoutMutation()

  const selectedKey = findSelectedKey(location.pathname)

  const confirmLogout = () => {
    modal.confirm({
      title: '로그아웃',
      content: '정말 로그아웃 하시겠어요?',
      okText: '로그아웃',
      cancelText: '취소',
      onOk: () => logoutMutate(),
    })
  }

  const adminFooter = (
    <div className={styles.adminFooter}>
      <span className={styles.adminName}>
        <UserOutlined />
        <Typography.Text strong>{auth?.name}</Typography.Text>
      </span>
      <Button type="text" size="small" icon={<LogoutOutlined />} onClick={confirmLogout}>
        로그아웃
      </Button>
    </div>
  )

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
          footer={adminFooter}
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
        <div className={styles.siderInner}>
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
            className={styles.menu}
          />
          {adminFooter}
        </div>
      </Layout.Sider>
      <Layout.Content className={styles.content}>
        <Outlet />
      </Layout.Content>
    </Layout>
  )
}
