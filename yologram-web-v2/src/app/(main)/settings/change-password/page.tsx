'use client'

import { useState } from 'react'
import { useRouter } from 'next/navigation'
import { Button, Input, Typography } from 'antd'
import { ArrowLeftOutlined } from '@ant-design/icons'
import useChangePasswordMutation from '@/queries/useChangePasswordMutation'
import RequireAuth from '@/components/auth/RequireAuth'
import styles from './ChangePassword.module.css'

const { Title } = Typography

export default function ChangePassword() {
  const router = useRouter()
  const { mutate, isPending } = useChangePasswordMutation()

  const [currentPassword, setCurrentPassword] = useState('')
  const [newPassword, setNewPassword] = useState('')
  const [confirmPassword, setConfirmPassword] = useState('')
  const [errors, setErrors] = useState<Record<string, string>>({})

  const validate = () => {
    const next: Record<string, string> = {}

    if (!currentPassword) next.currentPassword = '현재 비밀번호를 입력해주세요'
    if (!newPassword) next.newPassword = '새 비밀번호를 입력해주세요'
    else if (newPassword.length < 8 || newPassword.length > 20) next.newPassword = '비밀번호는 8~20자여야 합니다'
    if (!confirmPassword) next.confirmPassword = '새 비밀번호 확인을 입력해주세요'
    else if (newPassword !== confirmPassword) next.confirmPassword = '새 비밀번호가 일치하지 않습니다'

    setErrors(next)
    return Object.keys(next).length === 0
  }

  const handleSubmit = () => {
    if (!validate()) return
    mutate({ currentPassword, newPassword })
  }

  return (
    <RequireAuth>
      <div className={styles.container}>
        <div className={styles.header}>
          <button className={styles.backButton} onClick={() => router.push('/settings')}>
            <ArrowLeftOutlined />
          </button>
          <Title level={4} style={{ margin: 0 }}>비밀번호 변경</Title>
        </div>

        <div className={styles.form}>
          <div className={styles.field}>
            <label>현재 비밀번호</label>
            <Input.Password
              value={currentPassword}
              onChange={(e) => setCurrentPassword(e.target.value)}
              placeholder="현재 비밀번호를 입력해주세요"
              status={errors.currentPassword ? 'error' : undefined}
            />
            {errors.currentPassword && <div className={styles.error}>{errors.currentPassword}</div>}
          </div>

          <div className={styles.field}>
            <label>새 비밀번호</label>
            <Input.Password
              value={newPassword}
              onChange={(e) => setNewPassword(e.target.value)}
              placeholder="새 비밀번호를 입력해주세요 (8~20자)"
              status={errors.newPassword ? 'error' : undefined}
            />
            {errors.newPassword && <div className={styles.error}>{errors.newPassword}</div>}
          </div>

          <div className={styles.field}>
            <label>새 비밀번호 확인</label>
            <Input.Password
              value={confirmPassword}
              onChange={(e) => setConfirmPassword(e.target.value)}
              placeholder="새 비밀번호를 다시 입력해주세요"
              status={errors.confirmPassword ? 'error' : undefined}
            />
            {errors.confirmPassword && <div className={styles.error}>{errors.confirmPassword}</div>}
          </div>

          <Button type="primary" size="large" block onClick={handleSubmit} loading={isPending}>
            비밀번호 변경
          </Button>
        </div>
      </div>
    </RequireAuth>
  )
}
