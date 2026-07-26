import SubTabLayout from '../../components/common/SubTabLayout'

const tabs = [
  { key: 'users', label: '유저 관리' },
  { key: 'admin-users', label: '어드민 관리' },
]

/** UMS(유저 관리) 메뉴 서브탭 골격. 각 탭 내용(목록)은 후속 구현. */
export default function UmsPage() {
  return <SubTabLayout basePath="/ums" tabs={tabs} title="유저 관리" />
}
