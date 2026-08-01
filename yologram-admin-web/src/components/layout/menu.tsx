import type { ReactNode } from 'react'
import {
  DashboardOutlined,
  UserOutlined,
  TagsOutlined,
  FileTextOutlined,
  NotificationOutlined,
} from '@ant-design/icons'

export interface MenuChild {
  /** 라우트 경로 */
  key: string
  label: string
}

export interface MenuSection {
  /** 섹션 루트 경로 프리픽스 — 상단 메뉴 선택 매칭 기준 */
  key: string
  icon: ReactNode
  label: string
  children: MenuChild[]
}

/**
 * 2단 메뉴 정의 — 상단 바(최상위 분류) + 좌측 사이드바(현재 최상위의 하위 분류) 공용.
 * 데스크탑: Header 가로 메뉴(최상위) / Sider 세로 메뉴(하위), 모바일: Drawer에 그룹으로 2단 표시.
 */
export const MENU_SECTIONS: MenuSection[] = [
  {
    key: '/dashboard',
    icon: <DashboardOutlined />,
    label: '대시보드',
    children: [{ key: '/dashboard', label: '대시보드' }],
  },
  {
    key: '/ums',
    icon: <UserOutlined />,
    label: '유저 관리',
    children: [
      { key: '/ums/users', label: '유저 관리' },
      { key: '/ums/admin-users', label: '어드민 관리' },
    ],
  },
  {
    key: '/categories',
    icon: <TagsOutlined />,
    label: '카테고리 관리',
    children: [{ key: '/categories', label: '카테고리 관리' }],
  },
  {
    key: '/posts',
    icon: <FileTextOutlined />,
    label: '게시글 관리',
    children: [{ key: '/posts', label: '게시글 관리' }],
  },
  {
    key: '/news',
    icon: <NotificationOutlined />,
    label: '뉴스 관리',
    children: [
      { key: '/news/tech', label: '기술 뉴스' },
      { key: '/news/invest', label: '투자 뉴스' },
      { key: '/news/politics', label: '정치 뉴스' },
    ],
  },
]

/** 현재 경로가 속한 최상위 섹션 */
export function findSelectedSection(pathname: string): MenuSection | undefined {
  return MENU_SECTIONS.find((section) => pathname.startsWith(section.key))
}

/** 현재 경로에 해당하는 하위 메뉴 key */
export function findSelectedChildKey(pathname: string): string | undefined {
  return findSelectedSection(pathname)?.children.find((child) => pathname.startsWith(child.key))?.key
}
