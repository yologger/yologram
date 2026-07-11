import { Outlet, useLocation, useNavigate } from 'react-router'
import { Layout, Menu, Typography } from 'antd'
import {
  DashboardOutlined,
  UserOutlined,
  TagsOutlined,
  FileTextOutlined,
  NotificationOutlined,
} from '@ant-design/icons'
import styles from './AdminLayout.module.css'

const MENU_ITEMS = [
  { key: '/dashboard', icon: <DashboardOutlined />, label: '대시보드' },
  { key: '/users', icon: <UserOutlined />, label: '회원 관리' },
  { key: '/categories', icon: <TagsOutlined />, label: '카테고리 관리' },
  { key: '/posts', icon: <FileTextOutlined />, label: '게시글 관리' },
  { key: '/feeds', icon: <NotificationOutlined />, label: 'RSS 피드 관리' },
]

/**
 * 어드민 공통 레이아웃. 데스크탑 전용(반응형 분기 없음) — 좌측 사이드바 + 콘텐츠.
 */
export default function AdminLayout() {
  const navigate = useNavigate()
  const location = useLocation()

  const selectedKey = MENU_ITEMS.find((item) => location.pathname.startsWith(item.key))?.key

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
