import { useEffect, useState } from 'react'
import { App, Button, Form, Input, Modal, Table, Tag, Tooltip, Typography } from 'antd'
import { PlusOutlined } from '@ant-design/icons'
import { useQueryClient } from '@tanstack/react-query'
import { useAtomValue } from 'jotai'
import { authAtom } from '../../stores/auth'
import useAdminUsersQuery from '../../queries/useAdminUsersQuery'
import useCreateAdminUserMutation from '../../queries/useCreateAdminUserMutation'
import useDeleteAdminUserMutation from '../../queries/useDeleteAdminUserMutation'
import { getErrorMessage } from '../../lib/error'
import type { AdminUser, AdminUserStatus } from '../../apis/adminUsers'
import styles from './AdminUsersPage.module.css'

interface AdminUserFormValues {
  email: string
  name: string
  password: string
}

const STATUS_LABELS: Record<AdminUserStatus, string> = {
  ACTIVE: '활성',
  INACTIVE: '비활성',
  DELETED: '삭제됨',
}

const PAGE_SIZE = 10

/** 어드민 관리 — 어드민 계정 목록 조회(페이지네이션)·추가·삭제. 자기 자신은 삭제할 수 없다. */
export default function AdminUsersPage() {
  const [page, setPage] = useState(0)
  const { data: adminUsersPage, isLoading } = useAdminUsersQuery(page)
  const adminUsers = adminUsersPage?.data
  const [modalOpen, setModalOpen] = useState(false)
  const [form] = Form.useForm<AdminUserFormValues>()
  const { message, modal } = App.useApp()
  const queryClient = useQueryClient()
  const auth = useAtomValue(authAtom)

  const createMutation = useCreateAdminUserMutation()
  const deleteMutation = useDeleteAdminUserMutation()

  // Modal이 열릴 때 폼을 초기화한다.
  useEffect(() => {
    if (modalOpen) form.resetFields()
  }, [modalOpen, form])

  const invalidateAdminUsers = () => queryClient.invalidateQueries({ queryKey: ['adminUsers'] })

  const onSubmit = (values: AdminUserFormValues) => {
    createMutation.mutate(values, {
      onSuccess: () => {
        message.success('어드민을 추가했어요.')
        setModalOpen(false)
        invalidateAdminUsers()
      },
      onError: (error) => message.error(getErrorMessage(error)),
    })
  }

  const confirmDelete = (adminUser: AdminUser) => {
    modal.confirm({
      title: '어드민 삭제',
      content: `'${adminUser.name}' 어드민을 삭제할까요?`,
      okText: '삭제',
      okButtonProps: { danger: true },
      cancelText: '취소',
      onOk: () =>
        deleteMutation.mutateAsync(adminUser.uid).then(
          () => {
            message.success('어드민을 삭제했어요.')
            // 페이지의 마지막 항목을 삭제해 빈 페이지가 되면 이전 페이지로 보정
            if (adminUsers?.length === 1 && page > 0) setPage(page - 1)
            invalidateAdminUsers()
          },
          (error) => message.error(getErrorMessage(error)),
        ),
    })
  }

  const columns = [
    { title: 'UID', dataIndex: 'uid', width: 70 },
    { title: '이메일', dataIndex: 'email' },
    {
      title: '이름',
      dataIndex: 'name',
      render: (name: string, adminUser: AdminUser) => (
        <>
          {name}
          {adminUser.uid === auth?.uid && <Tag className={styles.meTag}>나</Tag>}
        </>
      ),
    },
    {
      title: '상태',
      dataIndex: 'status',
      width: 90,
      render: (status: AdminUserStatus) => (
        <Tag color={status === 'ACTIVE' ? 'blue' : 'default'}>{STATUS_LABELS[status]}</Tag>
      ),
    },
    {
      title: '가입일',
      dataIndex: 'joinedDate',
      width: 120,
      render: (joinedDate: string) => joinedDate.slice(0, 10),
    },
    {
      title: '',
      key: 'actions',
      width: 80,
      render: (_: unknown, adminUser: AdminUser) => {
        const isSelf = adminUser.uid === auth?.uid
        const button = (
          <Button
            type="link"
            size="small"
            danger
            disabled={isSelf}
            onClick={() => confirmDelete(adminUser)}
          >
            삭제
          </Button>
        )
        return isSelf ? <Tooltip title="자기 자신은 삭제할 수 없습니다">{button}</Tooltip> : button
      },
    },
  ]

  return (
    <div>
      <div className={styles.header}>
        <Typography.Title level={3} className={styles.title}>
          어드민 관리
        </Typography.Title>
        <Button type="primary" icon={<PlusOutlined />} onClick={() => setModalOpen(true)}>
          어드민 추가
        </Button>
      </div>
      <Table
        rowKey="uid"
        columns={columns}
        dataSource={adminUsers}
        loading={isLoading}
        locale={{ emptyText: '등록된 어드민이 없습니다' }}
        pagination={{
          current: page + 1,
          pageSize: PAGE_SIZE,
          total: adminUsersPage?.totalCount ?? 0,
          showSizeChanger: false,
          onChange: (nextPage) => setPage(nextPage - 1),
        }}
      />
      <Modal
        title="어드민 추가"
        open={modalOpen}
        onCancel={() => setModalOpen(false)}
        onOk={() => form.submit()}
        okText="추가"
        cancelText="취소"
        confirmLoading={createMutation.isPending}
      >
        <Form form={form} layout="vertical" onFinish={onSubmit}>
          <Form.Item
            name="email"
            label="이메일"
            rules={[
              { required: true, message: '이메일을 입력해주세요' },
              { type: 'email', message: '올바른 이메일 형식이 아닙니다' },
            ]}
          >
            <Input placeholder="admin@yologram.link" />
          </Form.Item>
          <Form.Item
            name="name"
            label="이름"
            rules={[
              { required: true, message: '이름을 입력해주세요' },
              { min: 2, message: '이름은 2~20자로 입력해주세요' },
              { max: 20, message: '이름은 2~20자로 입력해주세요' },
            ]}
          >
            <Input placeholder="이름" />
          </Form.Item>
          <Form.Item
            name="password"
            label="비밀번호"
            rules={[
              { required: true, message: '비밀번호를 입력해주세요' },
              { min: 8, message: '비밀번호는 8~20자로 입력해주세요' },
              { max: 20, message: '비밀번호는 8~20자로 입력해주세요' },
            ]}
          >
            <Input.Password placeholder="비밀번호" />
          </Form.Item>
        </Form>
      </Modal>
    </div>
  )
}
