import {
  DashboardOutlined,
  UserOutlined,
  TagsOutlined,
  FileTextOutlined,
  NotificationOutlined,
} from '@ant-design/icons'

/** 데스크탑 사이드바·모바일 Drawer 공용 메뉴 정의 */
export const MENU_ITEMS = [
  { key: '/dashboard', icon: <DashboardOutlined />, label: '대시보드' },
  { key: '/users', icon: <UserOutlined />, label: '회원 관리' },
  { key: '/categories', icon: <TagsOutlined />, label: '카테고리 관리' },
  { key: '/posts', icon: <FileTextOutlined />, label: '게시글 관리' },
  { key: '/feeds', icon: <NotificationOutlined />, label: 'RSS 피드 관리' },
]

export function findSelectedKey(pathname: string) {
  return MENU_ITEMS.find((item) => pathname.startsWith(item.key))?.key
}
