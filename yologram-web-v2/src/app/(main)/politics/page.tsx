import ComingSoon from '@/components/common/ComingSoon'

// 정치 준비 중: 탭 하위 경로는 (tabs)/layout이 ComingSoon으로 가로채고,
// 루트(/politics)는 (tabs) 밖이라 이 page가 직접 ComingSoon을 렌더한다.
// 구현 시작 시 아래 redirect를 복구한다.
export default function PoliticsPage() {
  return <ComingSoon title="정치" />
}

// import { redirect } from 'next/navigation'
//
// export default function PoliticsPage() {
//   redirect('/politics/news')
// }
