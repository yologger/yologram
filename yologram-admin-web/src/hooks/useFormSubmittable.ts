import { useEffect, useState } from 'react'
import { Form, type FormInstance } from 'antd'

/**
 * Ant Design Form의 모든 필드가 검증을 통과하는지 추적한다.
 * 제출 버튼 활성화 제어에 사용 (검증 실패 시 비활성, 통과 시 활성).
 */
export default function useFormSubmittable(form: FormInstance): boolean {
  const values = Form.useWatch([], form)
  const [submittable, setSubmittable] = useState(false)

  useEffect(() => {
    form.validateFields({ validateOnly: true })
      .then(() => setSubmittable(true))
      .catch(() => setSubmittable(false))
  }, [form, values])

  return submittable
}
