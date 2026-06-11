import { useState, useEffect } from 'react'
import { useNavigate } from 'react-router'
import { Button, Input, Typography } from 'antd'
import { ArrowLeftOutlined } from '@ant-design/icons'
import useUserQuery from '../../queries/useUserQuery'
import useUpdateProfileMutation from '../../queries/useUpdateProfileMutation'
import styles from './EditProfilePage.module.css'

const { Title } = Typography

export default function EditProfilePage() {
  const navigate = useNavigate()
  const { data: user } = useUserQuery()
  const { mutate, isPending } = useUpdateProfileMutation()

  const [nickname, setNickname] = useState('')
  const [errors, setErrors] = useState<Record<string, string>>({})

  useEffect(() => {
    if (user) {
      setNickname(user.nickname)
    }
  }, [user])

  const validate = () => {
    const next: Record<string, string> = {}

    if (!nickname) next.nickname = '닉네임을 입력해주세요'
    else if (nickname.length < 2 || nickname.length > 20) next.nickname = '닉네임은 2~20자여야 합니다'

    setErrors(next)
    return Object.keys(next).length === 0
  }

  const handleSubmit = () => {
    if (!validate()) return
    mutate({ nickname })
  }

  return (
    <div className={styles.container}>
      <div className={styles.header}>
        <button className={styles.backButton} onClick={() => navigate('/settings')}>
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
            status={errors.nickname ? 'error' : undefined}
          />
          {errors.nickname && <div className={styles.error}>{errors.nickname}</div>}
        </div>

        <Button type="primary" size="large" block onClick={handleSubmit} loading={isPending}>
          저장
        </Button>
      </div>
    </div>
  )
}
