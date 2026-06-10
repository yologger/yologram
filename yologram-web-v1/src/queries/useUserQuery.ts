import { useQuery } from '@tanstack/react-query'
import { useAtomValue } from 'jotai'
import { authAtom } from '../stores/auth'
import { getMe } from '../apis/auth'

export default function useUserQuery() {
  const auth = useAtomValue(authAtom)

  return useQuery({
    queryKey: ['user', 'me'],
    queryFn: getMe,
    enabled: !!auth,
  })
}
