'use client'

import { Avatar, Typography, List } from 'antd'
import {
  UserOutlined,
  BulbOutlined,
  EditOutlined,
  BookOutlined,
  FileTextOutlined,
  RightOutlined,
} from '@ant-design/icons'
import styles from './Settings.module.css'

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

export default function Settings() {
  return (
    <div className={styles.container}>
      <div className={styles.profile}>
        <Avatar size={64} icon={<UserOutlined />} className={styles.avatar} />
        <Title level={4} className={styles.username}>yologram</Title>
      </div>

      {sections.map((section) => (
        <div key={section.title} className={styles.section}>
          <Text type="secondary" className={styles.sectionTitle}>{section.title}</Text>
          <List
            dataSource={section.items}
            renderItem={(item) => (
              <List.Item className={styles.listItem} extra={<RightOutlined className={styles.arrow} />}>
                <List.Item.Meta
                  avatar={<span className={styles.itemIcon}>{item.icon}</span>}
                  title={item.label}
                  description={item.desc}
                />
              </List.Item>
            )}
          />
        </div>
      ))}

      <div className={styles.footer}>
        <Text type="secondary" className={styles.footerLink}>로그아웃</Text>
        <Text type="secondary"> | </Text>
        <Text type="secondary" className={styles.footerLink}>회원탈퇴</Text>
      </div>
    </div>
  )
}
