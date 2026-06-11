import { useMutation, useQueryClient } from '@tanstack/react-query'
import { useNavigate } from 'react-router'
import { message } from 'antd'
import { updateProfile, type UpdateProfileRequest } from '../apis/auth'
import { getErrorMessage } from '../lib/error'

export default function useUpdateProfileMutation() {
  const navigate = useNavigate()
  const queryClient = useQueryClient()

  return useMutation({
    mutationFn: (request: UpdateProfileRequest) => updateProfile(request),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['user', 'me'] })
      message.success('회원정보가 수정되었습니다.')
      navigate('/settings')
    },
    onError: (error) => {
      message.error(getErrorMessage(error))
    },
  })
}
