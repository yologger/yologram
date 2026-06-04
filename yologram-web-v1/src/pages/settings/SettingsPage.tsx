import { Avatar, Modal, Typography } from 'antd'
import {
  UserOutlined,
  BulbOutlined,
  EditOutlined,
  BookOutlined,
  FileTextOutlined,
  RightOutlined,
} from '@ant-design/icons'
import { useAtomValue } from 'jotai'
import { authAtom } from '../../stores/auth'
import useLogoutMutation from '../../queries/useLogoutMutation'
import styles from './SettingsPage.module.css'

const { Title, Text } = Typography

const sections = [
  {
    title: '환경 설정',
    items: [
      { icon: <BulbOutlined />, label: '다크 모드 설정', desc: '라이트·다크 모드를 선택해요' },
    ],
  },
  {
    title: '활동',
    items: [
      { icon: <EditOutlined />, label: '내가 쓴 글', desc: '내가 작성한 글을 관리해요' },
      { icon: <BookOutlined />, label: '저장한 글', desc: '북마크한 게시글을 관리해요' },
    ],
  },
  {
    title: '기타',
    items: [
      { icon: <FileTextOutlined />, label: '이용약관', desc: '' },
    ],
  },
]

export default function SettingsPage() {
  const auth = useAtomValue(authAtom)
  const { mutate: logoutMutate } = useLogoutMutation()

  return (
    <div className={styles.container}>
      <div className={styles.profile}>
        <Avatar size={64} icon={<UserOutlined />} className={styles.avatar} />
        <Title level={4} className={styles.username}>{auth?.nickname ?? 'yologram'}</Title>
      </div>

      {sections.map((section) => (
        <div key={section.title} className={styles.section}>
          <Text type="secondary" className={styles.sectionTitle}>{section.title}</Text>
          {section.items.map((item) => (
            <div key={item.label} className={styles.listItem}>
              <span className={styles.itemIcon}>{item.icon}</span>
              <div className={styles.itemContent}>
                <Text strong>{item.label}</Text>
                {item.desc && <Text type="secondary" className={styles.itemDesc}>{item.desc}</Text>}
              </div>
              <RightOutlined className={styles.arrow} />
            </div>
          ))}
        </div>
      ))}

      <div className={styles.footer}>
        <Text type="secondary" className={styles.footerLink} onClick={() => {
          Modal.confirm({
            title: '로그아웃',
            content: '정말 로그아웃 하시겠어요?',
            okText: '로그아웃',
            cancelText: '취소',
            onOk: () => logoutMutate(),
          })
        }}>로그아웃</Text>
        <Text type="secondary"> | </Text>
        <Text type="secondary" className={styles.footerLink}>회원탈퇴</Text>
      </div>
    </div>
  )
}
