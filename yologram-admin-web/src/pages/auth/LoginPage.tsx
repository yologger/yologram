import { Button, Form, Input, Typography } from 'antd'
import { MailOutlined, LockOutlined } from '@ant-design/icons'
import useLoginMutation from '../../queries/useLoginMutation'
import useFormSubmittable from '../../hooks/useFormSubmittable'
import styles from './LoginPage.module.css'

const { Title } = Typography

/** 어드민 로그인 페이지. 회원가입·비밀번호 찾기 없이 로그인만 제공한다. */
export default function LoginPage() {
  const [form] = Form.useForm()
  const submittable = useFormSubmittable(form)
  const { mutate, isPending } = useLoginMutation()

  const onFinish = (values: { email: string; password: string }) => {
    mutate(values)
  }

  return (
    <div className={styles.container}>
      <div className={styles.card}>
        <Title level={3} className={styles.title}>yologram admin</Title>
        <Form form={form} onFinish={onFinish} layout="vertical" size="large">
          <Form.Item name="email" rules={[{ required: true, message: '이메일을 입력해주세요' }]}>
            <Input prefix={<MailOutlined />} placeholder="이메일" />
          </Form.Item>
          <Form.Item name="password" rules={[{ required: true, message: '비밀번호를 입력해주세요' }]}>
            <Input.Password prefix={<LockOutlined />} placeholder="비밀번호" />
          </Form.Item>
          <Form.Item>
            <Button type="primary" htmlType="submit" block loading={isPending} disabled={!submittable}>
              로그인
            </Button>
          </Form.Item>
        </Form>
      </div>
    </div>
  )
}
