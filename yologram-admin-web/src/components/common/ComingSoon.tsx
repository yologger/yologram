import { Typography } from 'antd'
import { ToolOutlined } from '@ant-design/icons'
import styles from './ComingSoon.module.css'

interface ComingSoonProps {
  /** 상단 헤더에 표시할 페이지명 */
  title?: string
}

/**
 * 미구현 메뉴 진입 시 보여주는 "준비 중" 안내 화면.
 * 해당 메뉴 기능을 구현하면 Router에서 이 컴포넌트 매핑을 페이지 컴포넌트로 교체한다.
 */
export default function ComingSoon({ title }: ComingSoonProps) {
  return (
    <div>
      {title && <Typography.Title level={3}>{title}</Typography.Title>}
      <div className={styles.container}>
        <ToolOutlined className={styles.icon} />
        <Typography.Title level={4} className={styles.title}>
          페이지 준비 중입니다
        </Typography.Title>
        <Typography.Text type="secondary">곧 찾아뵙겠습니다.</Typography.Text>
      </div>
    </div>
  )
}
