'use client'

import { useMutation, useQueryClient } from '@tanstack/react-query'
import { useRouter } from 'next/navigation'
import { message } from 'antd'
import { updateProfile, type UpdateProfileRequest } from '@/apis/auth'
import { getErrorMessage } from '@/lib/error'

export default function useUpdateProfileMutation() {
  const router = useRouter()
  const queryClient = useQueryClient()

  return useMutation({
    mutationFn: (request: UpdateProfileRequest) => updateProfile(request),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['user', 'me'] })
      message.success('회원정보가 수정되었습니다.')
      router.push('/settings')
    },
    onError: (error) => {
      message.error(getErrorMessage(error))
    },
  })
}
