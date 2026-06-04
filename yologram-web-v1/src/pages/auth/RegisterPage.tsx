import { Button, Form, Input, Typography } from 'antd'
import { MailOutlined, LockOutlined, UserOutlined } from '@ant-design/icons'
import { Link } from 'react-router'
import styles from './LoginPage.module.css'

const { Title, Text } = Typography

export default function RegisterPage() {
  const onFinish = () => {
    // TODO: API 서버 구축 후 구현
  }

  return (
    <div className={styles.container}>
      <div className={styles.card}>
        <Title level={3} className={styles.title}>회원가입</Title>
        <Form onFinish={onFinish} layout="vertical" size="large">
          <Form.Item name="email" rules={[{ required: true, message: '이메일을 입력해주세요' }]}>
            <Input prefix={<MailOutlined />} placeholder="이메일" />
          </Form.Item>
          <Form.Item name="nickname" rules={[{ required: true, message: '닉네임을 입력해주세요' }]}>
            <Input prefix={<UserOutlined />} placeholder="닉네임" />
          </Form.Item>
          <Form.Item name="password" rules={[{ required: true, message: '비밀번호를 입력해주세요' }]}>
            <Input.Password prefix={<LockOutlined />} placeholder="비밀번호" />
          </Form.Item>
          <Form.Item>
            <Button type="primary" htmlType="submit" block>
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
