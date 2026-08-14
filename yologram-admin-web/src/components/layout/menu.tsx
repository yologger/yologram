import type { ReactNode } from 'react'
import type { MenuProps } from 'antd'
import {
  SoundOutlined,
  UserOutlined,
  TagsOutlined,
  FileTextOutlined,
  NotificationOutlined,
  SearchOutlined,
} from '@ant-design/icons'

export interface MenuChild {
  /** 라우트 경로. children이 있으면 SubMenu 식별자(경로 아님) */
  key: string
  label: string
  /** 있으면 SubMenu로 렌더되는 중첩 하위 메뉴 */
  children?: MenuChild[]
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
 * 사이드바 하위는 필요 시 SubMenu(중첩 children)로 한 단계 더 나뉜다.
 * 데스크탑: Header 가로 메뉴(최상위) / Sider 세로 메뉴(하위), 모바일: Drawer에 그룹으로 표시.
 */
export const MENU_SECTIONS: MenuSection[] = [
  {
    key: '/notices',
    icon: <SoundOutlined />,
    label: '공지',
    children: [{ key: '/notices', label: '공지 관리' }],
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
    key: '/posts',
    icon: <FileTextOutlined />,
    label: '게시글 관리',
    children: [{ key: '/posts', label: '게시글 관리' }],
  },
  {
    key: '/search',
    icon: <SearchOutlined />,
    label: '검색 관리',
    children: [
      {
        key: 'post-indexing',
        label: '게시글 인덱싱',
        children: [
          { key: '/search/tech/posts/indexing', label: '기술' },
          { key: '/search/politics/posts/indexing', label: '정치' },
          { key: '/search/invest/posts/indexing', label: '투자' },
        ],
      },
    ],
  },
  {
    key: '/news',
    icon: <NotificationOutlined />,
    label: '뉴스 관리',
    children: [
      {
        key: 'tech-news',
        label: '기술 뉴스',
        children: [
          { key: '/news/tech', label: '뉴스 관리' },
          { key: '/news/tech/sources', label: '소스 관리' },
        ],
      },
      { key: '/news/invest', label: '투자 뉴스' },
      { key: '/news/politics', label: '정치 뉴스' },
    ],
  },
  {
    key: '/categories',
    icon: <TagsOutlined />,
    label: '카테고리 관리',
    children: [{ key: '/categories', label: '카테고리 관리' }],
  },
]

/** 중첩 children을 평탄화해 라우트 경로를 갖는 리프 항목만 모은다 */
function collectLeaves(children: MenuChild[]): MenuChild[] {
  return children.flatMap((child) => (child.children ? collectLeaves(child.children) : [child]))
}

/** 현재 경로가 속한 최상위 섹션 */
export function findSelectedSection(pathname: string): MenuSection | undefined {
  return MENU_SECTIONS.find((section) => pathname.startsWith(section.key))
}

/** 현재 경로에 해당하는 하위 메뉴 key — 가장 긴 프리픽스 매칭(/news/tech vs /news/tech/sources 구분) */
export function findSelectedChildKey(pathname: string): string | undefined {
  const section = findSelectedSection(pathname)
  if (!section) return undefined
  return collectLeaves(section.children)
    .filter((leaf) => pathname.startsWith(leaf.key))
    .sort((a, b) => b.key.length - a.key.length)[0]?.key
}

/** 섹션의 첫 리프 경로 — 상단 메뉴 클릭 시 이동 대상 */
export function findFirstLeafKey(section: MenuSection): string {
  return collectLeaves(section.children)[0].key
}

/** 섹션 내 SubMenu key 목록 — 기본 펼침(defaultOpenKeys) 처리용 */
export function findSubMenuKeys(section: MenuSection | undefined): string[] {
  return section?.children.filter((child) => child.children).map((child) => child.key) ?? []
}

type AntdMenuItems = NonNullable<MenuProps['items']>

/** MenuChild 트리를 antd Menu items로 변환 — 중첩 children은 SubMenu */
export function toAntdMenuItems(children: MenuChild[]): AntdMenuItems {
  return children.map((child) =>
    child.children
      ? { key: child.key, label: child.label, children: toAntdMenuItems(child.children) }
      : { key: child.key, label: child.label },
  )
}
