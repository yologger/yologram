import { Button, Form, Input, Space, Typography } from 'antd'
import { MailOutlined, LockOutlined, UserOutlined, IdcardOutlined, SafetyOutlined } from '@ant-design/icons'
import { useState } from 'react'
import { Link } from 'react-router'
import useJoinMutation from '../../queries/useJoinMutation'
import useSendVerificationCodeMutation from '../../queries/useSendVerificationCodeMutation'
import useVerifyEmailMutation from '../../queries/useVerifyEmailMutation'
import useFormSubmittable from '../../hooks/useFormSubmittable'
import type { JoinRequest } from '../../apis/auth'
import styles from './LoginPage.module.css'

const { Title, Text } = Typography

export default function JoinPage() {
  const [form] = Form.useForm()
  const [codeSent, setCodeSent] = useState(false)
  const [emailVerified, setEmailVerified] = useState(false)
  const submittable = useFormSubmittable(form)

  const emailValue = Form.useWatch('email', form)
  const codeValue = Form.useWatch('code', form)
  const emailValid = /^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(emailValue ?? '')
  const codeValid = /^\d{6}$/.test(codeValue ?? '')

  const { mutate: join, isPending: isJoining } = useJoinMutation()
  const { mutate: sendCode, isPending: isSending } = useSendVerificationCodeMutation()
  const { mutate: verifyCode, isPending: isVerifying } = useVerifyEmailMutation()

  const handleSendCode = async () => {
    try {
      await form.validateFields(['email'])
    } catch {
      return
    }
    const email = form.getFieldValue('email')
    sendCode(email, { onSuccess: () => setCodeSent(true) })
  }

  const handleVerify = async () => {
    try {
      await form.validateFields(['code'])
    } catch {
      return
    }
    const email = form.getFieldValue('email')
    const code = form.getFieldValue('code')
    verifyCode({ email, code }, { onSuccess: () => setEmailVerified(true) })
  }

  const onValuesChange = (changed: Partial<JoinRequest & { code: string }>) => {
    if ('email' in changed && (codeSent || emailVerified)) {
      setCodeSent(false)
      setEmailVerified(false)
      form.setFieldValue('code', undefined)
    }
  }

  const onFinish = (values: JoinRequest & { code?: string }) => {
    const { code: _code, ...request } = values
    join(request)
  }

  return (
    <div className={styles.container}>
      <div className={styles.card}>
        <Title level={3} className={styles.title}>회원가입</Title>
        <Form form={form} onFinish={onFinish} onValuesChange={onValuesChange} layout="vertical" size="large">
          <Form.Item name="email" rules={[
            { required: true, message: '이메일을 입력해주세요' },
            { type: 'email', message: '올바른 이메일 형식이 아닙니다' },
          ]}>
            <Space.Compact block>
              <Input prefix={<MailOutlined />} placeholder="이메일" disabled={emailVerified} />
              <Button onClick={handleSendCode} loading={isSending} disabled={emailVerified || !emailValid}>
                {codeSent ? '재발송' : '인증코드 발송'}
              </Button>
            </Space.Compact>
          </Form.Item>

          {codeSent && (
            <Form.Item name="code" rules={[
              { required: true, message: '인증 코드를 입력해주세요' },
              { len: 6, message: '인증 코드는 6자리입니다' },
            ]}>
              <Space.Compact block>
                <Input prefix={<SafetyOutlined />} placeholder="인증 코드 6자리" disabled={emailVerified} />
                <Button onClick={handleVerify} loading={isVerifying} disabled={emailVerified || !codeValid}>
                  {emailVerified ? '인증 완료' : '인증 확인'}
                </Button>
              </Space.Compact>
            </Form.Item>
          )}

          {emailVerified && (
            <Text type="success" className={styles.verified}>이메일 인증 완료</Text>
          )}

          <Form.Item name="name" rules={[
            { required: true, message: '이름을 입력해주세요' },
            { min: 2, max: 20, message: '이름은 2~20자여야 합니다' },
          ]}>
            <Input prefix={<IdcardOutlined />} placeholder="이름" disabled={!emailVerified} />
          </Form.Item>
          <Form.Item name="nickname" rules={[
            { required: true, message: '닉네임을 입력해주세요' },
            { min: 2, max: 20, message: '닉네임은 2~20자여야 합니다' },
          ]}>
            <Input prefix={<UserOutlined />} placeholder="닉네임" disabled={!emailVerified} />
          </Form.Item>
          <Form.Item name="password" rules={[
            { required: true, message: '비밀번호를 입력해주세요' },
            { min: 8, max: 20, message: '비밀번호는 8~20자여야 합니다' },
          ]}>
            <Input.Password prefix={<LockOutlined />} placeholder="비밀번호" disabled={!emailVerified} />
          </Form.Item>
          <Form.Item>
            <Button type="primary" htmlType="submit" block loading={isJoining} disabled={!emailVerified || !submittable}>
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
