import { useNavigate } from 'react-router'
import { Button, Typography } from 'antd'
import { ToolOutlined } from '@ant-design/icons'
import styles from './ComingSoon.module.css'

/**
 * 미구현 섹션 진입 시 보여주는 "준비 중" 안내 화면.
 * 해당 섹션 기능을 구현하면 Router에서 이 컴포넌트 매핑을 제거하고 기존 라우트를 복구한다.
 */
export default function ComingSoon() {
  const navigate = useNavigate()

  return (
    <div className={styles.container}>
      <ToolOutlined className={styles.icon} />
      <Typography.Title level={4} className={styles.title}>
        {'페이지 준비 중입니다'}
      </Typography.Title>
      <Typography.Text type="secondary">곧 찾아뵙겠습니다.</Typography.Text>
      <Button type="primary" className={styles.button} onClick={() => navigate('/tech')}>
        홈으로
      </Button>
    </div>
  )
}
