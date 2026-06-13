'use client'

import { useEffect, useState } from 'react'
import { useRouter } from 'next/navigation'
import { Button, Input, Typography } from 'antd'
import { ArrowLeftOutlined } from '@ant-design/icons'
import useUserQuery from '@/queries/useUserQuery'
import useUpdateProfileMutation from '@/queries/useUpdateProfileMutation'
import RequireAuth from '@/components/auth/RequireAuth'
import styles from './EditProfile.module.css'

const { Title } = Typography

export default function EditProfile() {
  const router = useRouter()
  const { data: user } = useUserQuery()
  const { mutate, isPending } = useUpdateProfileMutation()

  const [nickname, setNickname] = useState('')
  const [errors, setErrors] = useState<Record<string, string>>({})

  useEffect(() => {
    if (user) setNickname(user.nickname)
  }, [user])

  const validate = () => {
    const next: Record<string, string> = {}

    if (!nickname.trim()) next.nickname = '닉네임을 입력해주세요'
    else if (nickname.trim().length < 2 || nickname.trim().length > 20) next.nickname = '닉네임은 2~20자여야 합니다'

    setErrors(next)
    return Object.keys(next).length === 0
  }

  const handleSubmit = () => {
    if (!validate()) return
    mutate({ nickname: nickname.trim() })
  }

  const isValid = nickname.trim().length >= 2 && nickname.trim().length <= 20

  return (
    <RequireAuth>
      <div className={styles.container}>
        <div className={styles.header}>
          <button className={styles.backButton} onClick={() => router.push('/settings')}>
            <ArrowLeftOutlined />
          </button>
          <Title level={4} style={{ margin: 0 }}>회원정보 수정</Title>
        </div>

        <div className={styles.form}>
          <div className={styles.field}>
            <label>이메일</label>
            <Input value={user?.email ?? ''} disabled />
          </div>

          <div className={styles.field}>
            <label>이름</label>
            <Input value={user?.name ?? ''} disabled />
          </div>

          <div className={styles.field}>
            <label>닉네임</label>
            <Input
              value={nickname}
              onChange={(e) => setNickname(e.target.value)}
              placeholder="닉네임을 입력해주세요 (2~20자)"
              maxLength={20}
              status={errors.nickname ? 'error' : undefined}
            />
            {errors.nickname && <div className={styles.error}>{errors.nickname}</div>}
          </div>

          <Button type="primary" size="large" block onClick={handleSubmit} loading={isPending} disabled={!isValid}>
            저장
          </Button>
        </div>
      </div>
    </RequireAuth>
  )
}
