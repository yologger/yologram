import { useState } from 'react'
import { Outlet, useLocation, useNavigate } from 'react-router'
import { App, Avatar, Button, Drawer, Dropdown, Layout, Menu, Typography } from 'antd'
import { LogoutOutlined, MenuOutlined, UserOutlined } from '@ant-design/icons'
import { useAtomValue } from 'jotai'
import { authAtom } from '../../stores/auth'
import useLogoutMutation from '../../queries/useLogoutMutation'
import useIsMobile from '../../hooks/useIsMobile'
import { MENU_SECTIONS, findSelectedSection, findSelectedChildKey } from './menu'
import styles from './AdminLayout.module.css'

/**
 * 어드민 공통 레이아웃. 반응형 —
 * 데스크탑: 상단 바(최상위 분류 가로 메뉴 + 어드민 Dropdown 로그아웃) + 좌측 사이드바(현재 최상위의 하위 분류).
 * 모바일: 상단 헤더의 햄버거 버튼으로 토글하는 Drawer(2단 그룹 메뉴 + 하단 어드민 이름·로그아웃).
 */
export default function AdminLayout() {
  const isMobile = useIsMobile()
  const [drawerOpen, setDrawerOpen] = useState(false)
  const navigate = useNavigate()
  const location = useLocation()
  const auth = useAtomValue(authAtom)
  const { modal } = App.useApp()
  const { mutate: logoutMutate } = useLogoutMutation()

  const selectedSection = findSelectedSection(location.pathname)
  const selectedChildKey = findSelectedChildKey(location.pathname)

  const confirmLogout = () => {
    modal.confirm({
      title: '로그아웃',
      content: '정말 로그아웃 하시겠어요?',
      okText: '로그아웃',
      cancelText: '취소',
      onOk: () => logoutMutate(),
    })
  }

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
          footer={
            <div className={styles.adminFooter}>
              <span className={styles.adminName}>
                <UserOutlined />
                <Typography.Text strong>{auth?.name}</Typography.Text>
              </span>
              <Button type="text" size="small" icon={<LogoutOutlined />} onClick={confirmLogout}>
                로그아웃
              </Button>
            </div>
          }
        >
          <Menu
            mode="inline"
            items={MENU_SECTIONS.map((section) => ({
              type: 'group' as const,
              key: section.key,
              label: section.label,
              children: section.children.map((child) => ({ key: child.key, label: child.label })),
            }))}
            selectedKeys={selectedChildKey ? [selectedChildKey] : []}
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
      <Layout.Header className={styles.header}>
        <Typography.Title level={4} className={styles.logoTitle}>
          yologram admin
        </Typography.Title>
        <Menu
          mode="horizontal"
          className={styles.topMenu}
          items={MENU_SECTIONS.map(({ key, icon, label }) => ({ key, icon, label }))}
          selectedKeys={selectedSection ? [selectedSection.key] : []}
          onClick={({ key }) => {
            const section = MENU_SECTIONS.find((s) => s.key === key)
            if (section) navigate(section.children[0].key)
          }}
        />
        <Dropdown
          trigger={['click']}
          menu={{
            items: [{ key: 'logout', icon: <LogoutOutlined />, label: '로그아웃' }],
            onClick: ({ key }) => {
              if (key === 'logout') confirmLogout()
            },
          }}
        >
          <button type="button" className={styles.adminTrigger}>
            <Avatar size="small" icon={<UserOutlined />} />
            <Typography.Text strong>{auth?.name}</Typography.Text>
          </button>
        </Dropdown>
      </Layout.Header>
      <Layout>
        <Layout.Sider theme="light" width={220} className={styles.sider}>
          <Menu
            mode="inline"
            items={selectedSection?.children.map((child) => ({ key: child.key, label: child.label })) ?? []}
            selectedKeys={selectedChildKey ? [selectedChildKey] : []}
            onClick={({ key }) => navigate(key)}
          />
        </Layout.Sider>
        <Layout.Content className={styles.content}>
          <Outlet />
        </Layout.Content>
      </Layout>
    </Layout>
  )
}
