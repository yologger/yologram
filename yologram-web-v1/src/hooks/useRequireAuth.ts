import { App } from 'antd'
import { useAtomValue } from 'jotai'
import { useLocation, useNavigate } from 'react-router'
import { authAtom } from '../stores/auth'

/**
 * 비로그인 사용자의 액션(좋아요·댓글 등) 진입 시 로그인 유도 모달을 띄우는 공용 훅.
 * 반환된 requireAuth()가 true면 인증 상태이므로 그대로 진행,
 * false면 로그인 유도 모달을 띄우고 호출부는 중단한다.
 * 모달에서 로그인을 선택하면 returnTo(state)를 넘겨 로그인 후 복귀한다.
 * returnTo 인자를 주면 그 경로로(예: 글쓰기 진입), 없으면 현재 경로로 복귀한다.
 */
export default function useRequireAuth() {
  const auth = useAtomValue(authAtom)
  const { modal } = App.useApp()
  const navigate = useNavigate()
  const location = useLocation()

  const requireAuth = (returnTo?: string): boolean => {
    if (auth != null) return true
    modal.confirm({
      title: '로그인이 필요해요',
      content: '좋아요와 댓글은 로그인 후 이용할 수 있어요.',
      okText: '로그인',
      cancelText: '취소',
      onOk: () => {
        // 로그인 성공 후 복귀 경로 — 지정 시 그 경로, 미지정 시 현재 화면
        navigate('/login', {
          state: { returnTo: returnTo ?? location.pathname + location.search },
        })
      },
    })
    return false
  }

  return requireAuth
}
