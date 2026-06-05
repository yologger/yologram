import { Button, Form, Input, Typography } from 'antd'
import { MailOutlined, LockOutlined, UserOutlined, IdcardOutlined } from '@ant-design/icons'
import { Link } from 'react-router'
import useJoinMutation from '../../queries/useJoinMutation'
import type { JoinRequest } from '../../apis/auth'
import styles from './LoginPage.module.css'

const { Title, Text } = Typography

export default function JoinPage() {
  const { mutate, isPending } = useJoinMutation()

  const onFinish = (values: JoinRequest) => {
    mutate(values)
  }

  return (
    <div className={styles.container}>
      <div className={styles.card}>
        <Title level={3} className={styles.title}>회원가입</Title>
        <Form onFinish={onFinish} layout="vertical" size="large">
          <Form.Item name="email" rules={[
            { required: true, message: '이메일을 입력해주세요' },
            { type: 'email', message: '올바른 이메일 형식이 아닙니다' },
          ]}>
            <Input prefix={<MailOutlined />} placeholder="이메일" />
          </Form.Item>
          <Form.Item name="name" rules={[
            { required: true, message: '이름을 입력해주세요' },
            { min: 2, max: 20, message: '이름은 2~20자여야 합니다' },
          ]}>
            <Input prefix={<IdcardOutlined />} placeholder="이름" />
          </Form.Item>
          <Form.Item name="nickname" rules={[
            { required: true, message: '닉네임을 입력해주세요' },
            { min: 2, max: 20, message: '닉네임은 2~20자여야 합니다' },
          ]}>
            <Input prefix={<UserOutlined />} placeholder="닉네임" />
          </Form.Item>
          <Form.Item name="password" rules={[
            { required: true, message: '비밀번호를 입력해주세요' },
            { min: 8, max: 20, message: '비밀번호는 8~20자여야 합니다' },
          ]}>
            <Input.Password prefix={<LockOutlined />} placeholder="비밀번호" />
          </Form.Item>
          <Form.Item>
            <Button type="primary" htmlType="submit" block loading={isPending}>
              회원가입
            </Button>
          </Form.Item>
        </Form>
        <div className={styles.links}>
          <Text type="secondary">이미 계정이 있으신가요? <Link to="/login">로그인</Link></Text>
        </div>
      </div>
    </div>
  )
}
