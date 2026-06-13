import { Button, Form, Input, Space, Typography } from 'antd'
import { MailOutlined, LockOutlined, SafetyOutlined } from '@ant-design/icons'
import { useState } from 'react'
import { Link } from 'react-router'
import useSendPasswordResetCodeMutation from '../../queries/useSendPasswordResetCodeMutation'
import useVerifyPasswordResetCodeMutation from '../../queries/useVerifyPasswordResetCodeMutation'
import useConfirmPasswordResetMutation from '../../queries/useConfirmPasswordResetMutation'
import styles from './LoginPage.module.css'

const { Title, Text } = Typography

interface FormValues {
  email: string
  code: string
  newPassword: string
  confirmPassword: string
}

export default function ForgotPasswordPage() {
  const [form] = Form.useForm()
  const [codeSent, setCodeSent] = useState(false)
  const [codeVerified, setCodeVerified] = useState(false)

  const { mutate: sendCode, isPending: isSending } = useSendPasswordResetCodeMutation()
  const { mutate: verifyCode, isPending: isVerifying } = useVerifyPasswordResetCodeMutation()
  const { mutate: confirmReset, isPending: isConfirming } = useConfirmPasswordResetMutation()

  const emailValue = Form.useWatch('email', form)
  const codeValue = Form.useWatch('code', form)
  const newPasswordValue = Form.useWatch('newPassword', form)
  const confirmPasswordValue = Form.useWatch('confirmPassword', form)

  const emailValid = /^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(emailValue ?? '')
  const codeValid = /^\d{6}$/.test(codeValue ?? '')
  const passwordValid =
    (newPasswordValue ?? '').length >= 8 &&
    (newPasswordValue ?? '').length <= 20 &&
    newPasswordValue === confirmPasswordValue

  const handleSendCode = async () => {
    try {
      await form.validateFields(['email'])
    } catch {
      return
    }
    sendCode(form.getFieldValue('email'), { onSuccess: () => setCodeSent(true) })
  }

  const handleVerify = async () => {
    try {
      await form.validateFields(['code'])
    } catch {
      return
    }
    verifyCode(
      { email: form.getFieldValue('email'), code: form.getFieldValue('code') },
      { onSuccess: () => setCodeVerified(true) },
    )
  }

  const onValuesChange = (changed: Partial<FormValues>) => {
    if ('email' in changed && (codeSent || codeVerified)) {
      setCodeSent(false)
      setCodeVerified(false)
      form.setFieldsValue({ code: undefined, newPassword: undefined, confirmPassword: undefined })
    }
  }

  const onFinish = (values: FormValues) => {
    if (!codeVerified) return
    confirmReset({ email: values.email, code: values.code, newPassword: values.newPassword })
  }

  return (
    <div className={styles.container}>
      <div className={styles.card}>
        <Title level={3} className={styles.title}>비밀번호 찾기</Title>
        <Form form={form} onFinish={onFinish} onValuesChange={onValuesChange} layout="vertical" size="large">
          <Form.Item name="email" rules={[
            { required: true, message: '이메일을 입력해주세요' },
            { type: 'email', message: '올바른 이메일 형식이 아닙니다' },
          ]}>
            <Space.Compact block>
              <Input prefix={<MailOutlined />} placeholder="이메일" disabled={codeVerified} />
              <Button onClick={handleSendCode} loading={isSending} disabled={codeVerified || !emailValid}>
                {codeSent ? '재발송' : '코드 발송'}
              </Button>
            </Space.Compact>
          </Form.Item>

          {codeSent && (
            <Form.Item name="code" rules={[
              { required: true, message: '인증 코드를 입력해주세요' },
              { len: 6, message: '인증 코드는 6자리입니다' },
            ]}>
              <Space.Compact block>
                <Input prefix={<SafetyOutlined />} placeholder="인증 코드 6자리" disabled={codeVerified} />
                <Button onClick={handleVerify} loading={isVerifying} disabled={codeVerified || !codeValid}>
                  {codeVerified ? '확인 완료' : '인증 확인'}
                </Button>
              </Space.Compact>
            </Form.Item>
          )}

          {codeVerified && (
            <>
              <Text type="success" className={styles.verified}>코드 확인 완료</Text>
              <Form.Item name="newPassword" rules={[
                { required: true, message: '새 비밀번호를 입력해주세요' },
                { min: 8, max: 20, message: '비밀번호는 8~20자여야 합니다' },
              ]}>
                <Input.Password prefix={<LockOutlined />} placeholder="새 비밀번호" />
              </Form.Item>
              <Form.Item name="confirmPassword" dependencies={['newPassword']} rules={[
                { required: true, message: '새 비밀번호를 다시 입력해주세요' },
                ({ getFieldValue }) => ({
                  validator(_, value) {
                    if (!value || getFieldValue('newPassword') === value) return Promise.resolve()
                    return Promise.reject(new Error('새 비밀번호가 일치하지 않습니다'))
                  },
                }),
              ]}>
                <Input.Password prefix={<LockOutlined />} placeholder="새 비밀번호 확인" />
              </Form.Item>
              <Form.Item>
                <Button type="primary" htmlType="submit" block loading={isConfirming} disabled={!passwordValid}>
                  비밀번호 변경
                </Button>
              </Form.Item>
            </>
          )}
        </Form>
        <div className={styles.links}>
          <Text type="secondary"><Link to="/login">로그인으로 돌아가기</Link></Text>
        </div>
      </div>
    </div>
  )
}
