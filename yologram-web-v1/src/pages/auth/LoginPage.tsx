import { Button, Form, Input, Typography } from 'antd'
import { MailOutlined, LockOutlined } from '@ant-design/icons'
import { Link } from 'react-router'
import useLoginMutation from '../../queries/useLoginMutation'
import styles from './LoginPage.module.css'

const { Title, Text } = Typography

export default function LoginPage() {
  const { mutate, isPending } = useLoginMutation()

  const onFinish = (values: { email: string; password: string }) => {
    mutate(values)
  }

  return (
    <div className={styles.container}>
      <div className={styles.card}>
        <Title level={3} className={styles.title}>로그인</Title>
        <Form onFinish={onFinish} layout="vertical" size="large">
          <Form.Item name="email" rules={[{ required: true, message: '이메일을 입력해주세요' }]}>
            <Input prefix={<MailOutlined />} placeholder="이메일" />
          </Form.Item>
          <Form.Item name="password" rules={[{ required: true, message: '비밀번호를 입력해주세요' }]}>
            <Input.Password prefix={<LockOutlined />} placeholder="비밀번호" />
          </Form.Item>
          <Form.Item>
            <Button type="primary" htmlType="submit" block loading={isPending}>
              로그인
            </Button>
          </Form.Item>
        </Form>
        <div className={styles.links}>
          <Text type="secondary">계정이 없으신가요? <Link to="/register">회원가입</Link></Text>
        </div>
        <Text type="secondary" className={styles.hint}>
          테스트: test@yologram.link / password
        </Text>
      </div>
    </div>
  )
}
