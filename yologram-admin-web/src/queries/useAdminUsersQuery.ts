import { keepPreviousData, useQuery } from '@tanstack/react-query'
import { getAdminUsers } from '../apis/adminUsers'

export default function useAdminUsersQuery(page: number) {
  return useQuery({
    queryKey: ['adminUsers', page],
    queryFn: () => getAdminUsers(page),
    // 페이지 이동 시 이전 목록을 유지해 깜빡임을 막는다
    placeholderData: keepPreviousData,
  })
}
