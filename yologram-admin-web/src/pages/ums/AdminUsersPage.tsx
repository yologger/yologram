import { useEffect, useState } from 'react'
import { App, Button, Form, Input, Modal, Switch, Table, Tag, Tooltip, Typography } from 'antd'
import { PlusOutlined } from '@ant-design/icons'
import { useQueryClient } from '@tanstack/react-query'
import { useAtomValue } from 'jotai'
import { authAtom } from '../../stores/auth'
import useAdminUsersQuery from '../../queries/useAdminUsersQuery'
import useCreateAdminUserMutation from '../../queries/useCreateAdminUserMutation'
import useDeleteAdminUserMutation from '../../queries/useDeleteAdminUserMutation'
import useUpdateAdminUserStatusMutation from '../../queries/useUpdateAdminUserStatusMutation'
import { getErrorMessage } from '../../lib/error'
import type { AdminRole } from '../../stores/auth'
import type { AdminUser, AdminUserStatus } from '../../apis/adminUsers'
import styles from './AdminUsersPage.module.css'

interface AdminUserFormValues {
  email: string
  name: string
  password: string
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
  const statusMutation = useUpdateAdminUserStatusMutation()

  /** 내가 OWNER면 상태 토글 가능, ADMIN이면 읽기 전용 */
  const isOwnerViewer = auth?.role === 'OWNER'

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

  const onToggleStatus = (adminUser: AdminUser, checked: boolean) => {
    statusMutation.mutate(
      { uid: adminUser.uid, status: checked ? 'ACTIVE' : 'INACTIVE' },
      {
        onSuccess: () => invalidateAdminUsers(),
        // 스위치는 목록 데이터 기반이라 실패 시 자동으로 원래 상태를 유지한다.
        onError: (error) => message.error(getErrorMessage(error)),
      },
    )
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
    {
      title: '이메일',
      dataIndex: 'email',
      render: (email: string, adminUser: AdminUser) => (
        <>
          {email}
          {adminUser.uid === auth?.uid && <Tag className={styles.meTag}>나</Tag>}
        </>
      ),
    },
    { title: '이름', dataIndex: 'name' },
    {
      title: '역할',
      dataIndex: 'role',
      width: 90,
      render: (role: AdminRole) => (
        <Tag color={role === 'OWNER' ? 'gold' : 'default'}>{role}</Tag>
      ),
    },
    {
      title: '상태',
      dataIndex: 'status',
      width: 90,
      render: (status: AdminUserStatus, adminUser: AdminUser) => {
        // ADMIN 시점: 읽기 전용 Tag
        if (!isOwnerViewer) {
          return (
            <Tag color={status === 'ACTIVE' ? 'blue' : 'default'}>
              {status === 'ACTIVE' ? '활성' : '비활성'}
            </Tag>
          )
        }

        // OWNER 시점: 활성/비활성 토글 Switch (OWNER 계정은 변경 불가)
        const isTargetOwner = adminUser.role === 'OWNER'
        const statusSwitch = (
          <Switch
            checked={status === 'ACTIVE'}
            disabled={isTargetOwner}
            aria-label={`${adminUser.name} 활성`}
            loading={statusMutation.isPending && statusMutation.variables?.uid === adminUser.uid}
            onChange={(checked) => onToggleStatus(adminUser, checked)}
          />
        )
        return isTargetOwner ? (
          <Tooltip title="OWNER 계정은 변경할 수 없습니다">{statusSwitch}</Tooltip>
        ) : (
          statusSwitch
        )
      },
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
        const isOwner = adminUser.role === 'OWNER'
        const button = (
          <Button
            type="link"
            size="small"
            danger
            disabled={isSelf || isOwner}
            onClick={() => confirmDelete(adminUser)}
          >
            삭제
          </Button>
        )
        // 본인이면서 OWNER인 경우 본인 안내를 우선한다
        if (isSelf) return <Tooltip title="자기 자신은 삭제할 수 없습니다">{button}</Tooltip>
        if (isOwner) return <Tooltip title="OWNER 계정은 삭제할 수 없습니다">{button}</Tooltip>
        return button
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
