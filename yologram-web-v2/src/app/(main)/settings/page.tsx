'use client'

import { Avatar, Modal, Typography } from 'antd'
import {
  UserOutlined,
  BellOutlined,
  LockOutlined,
  BulbOutlined,
  EditOutlined,
  BookOutlined,
  FileTextOutlined,
  RightOutlined,
} from '@ant-design/icons'
import { useRouter } from 'next/navigation'
import { useAtomValue } from 'jotai'
import { authAtom } from '@/stores/auth'
import useLogoutMutation from '@/queries/useLogoutMutation'
import useWithdrawMutation from '@/queries/useWithdrawMutation'
import useUserQuery from '@/queries/useUserQuery'
import RequireAuth from '@/components/auth/RequireAuth'
import styles from './Settings.module.css'

const { Title, Text } = Typography

const sections = [
  {
    title: '계정',
    items: [
      { icon: <UserOutlined />, label: '회원정보 수정', desc: '이름, 닉네임 등을 변경해요', path: '/settings/edit-profile' },
      { icon: <LockOutlined />, label: '비밀번호 변경', desc: '비밀번호를 변경해요', path: '/settings/change-password' },
    ],
  },
  {
    title: '환경 설정',
    items: [
      { icon: <BellOutlined />, label: '알림 설정', desc: '푸시 알림을 관리해요', path: '' },
      { icon: <BulbOutlined />, label: '다크 모드 설정', desc: '라이트·다크 모드를 선택해요', path: '' },
    ],
  },
  {
    title: '활동',
    items: [
      { icon: <EditOutlined />, label: '내가 쓴 글', desc: '내가 작성한 글을 관리해요', path: '' },
      { icon: <BookOutlined />, label: '저장한 글', desc: '북마크한 게시글을 관리해요', path: '' },
    ],
  },
  {
    title: '기타',
    items: [
      { icon: <FileTextOutlined />, label: '이용약관', desc: '', path: '' },
    ],
  },
]

export default function Settings() {
  const router = useRouter()
  const auth = useAtomValue(authAtom)
  const { data: user } = useUserQuery()
  const { mutate: logoutMutate } = useLogoutMutation()
  const { mutate: withdrawMutate } = useWithdrawMutation()

  return (
    <RequireAuth>
      <div className={styles.container}>
        <div className={styles.profile}>
          <Avatar size={64} icon={<UserOutlined />} className={styles.avatar} />
          <Title level={4} className={styles.username}>{user?.nickname ?? auth?.nickname ?? 'yologram'}</Title>
        </div>

        {sections.map((section) => (
          <div key={section.title} className={styles.section}>
            <Text type="secondary" className={styles.sectionTitle}>{section.title}</Text>
            {section.items.map((item) => (
              <div key={item.label} className={styles.listItem} onClick={() => item.path && router.push(item.path)}>
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
          <Text type="secondary" className={styles.footerLink} onClick={() => {
            Modal.confirm({
              title: '회원탈퇴',
              content: '정말 탈퇴하시겠어요? 되돌릴 수 없어요.',
              okText: '탈퇴',
              okButtonProps: { danger: true },
              cancelText: '취소',
              onOk: () => withdrawMutate(),
            })
          }}>회원탈퇴</Text>
        </div>
      </div>
    </RequireAuth>
  )
}
