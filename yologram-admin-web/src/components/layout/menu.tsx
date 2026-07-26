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
  { key: '/ums/users', icon: <UserOutlined />, label: '유저 관리' },
  { key: '/categories', icon: <TagsOutlined />, label: '카테고리 관리' },
  { key: '/posts', icon: <FileTextOutlined />, label: '게시글 관리' },
  { key: '/feeds', icon: <NotificationOutlined />, label: 'RSS 피드 관리' },
]

/** 메뉴 선택 매칭용 경로 프리픽스 — 메뉴 key와 다른 하위 경로(서브탭 등)를 해당 메뉴에 매핑한다. */
const MENU_MATCH_PREFIXES: Record<string, string> = {
  '/ums/users': '/ums',
}

export function findSelectedKey(pathname: string) {
  return MENU_ITEMS.find((item) => pathname.startsWith(MENU_MATCH_PREFIXES[item.key] ?? item.key))?.key
}
