import { useEffect, useState } from 'react'
import { App, Button, Form, Input, Modal, Switch, Table, Typography } from 'antd'
import { PlusOutlined } from '@ant-design/icons'
import { useQueryClient } from '@tanstack/react-query'
import useNewsSourcesQuery from '../../queries/useNewsSourcesQuery'
import useCreateNewsSourceMutation from '../../queries/useCreateNewsSourceMutation'
import useUpdateNewsSourceMutation from '../../queries/useUpdateNewsSourceMutation'
import useDeleteNewsSourceMutation from '../../queries/useDeleteNewsSourceMutation'
import { getErrorMessage } from '../../lib/error'
import type { NewsSource } from '../../apis/newsSources'
import styles from './NewsSourcesPage.module.css'

interface SourceFormValues {
  name: string
  url: string
  isActive: boolean
}

/** 뉴스 소스 관리 — 수집 대상 소스 목록 조회·추가·수정·삭제·활성 토글. */
export default function NewsSourcesPage() {
  const { data: sources, isLoading } = useNewsSourcesQuery()
  const [modalOpen, setModalOpen] = useState(false)
  const [editingSource, setEditingSource] = useState<NewsSource | null>(null)
  const [form] = Form.useForm<SourceFormValues>()
  const { message, modal } = App.useApp()
  const queryClient = useQueryClient()

  const createMutation = useCreateNewsSourceMutation()
  const updateMutation = useUpdateNewsSourceMutation()
  const toggleMutation = useUpdateNewsSourceMutation()
  const deleteMutation = useDeleteNewsSourceMutation()

  // Modal이 열릴 때 폼 상태를 준비한다 (수정: prefill / 추가: 초기화).
  useEffect(() => {
    if (!modalOpen) return
    if (editingSource) {
      form.setFieldsValue({
        name: editingSource.name,
        url: editingSource.url,
        isActive: editingSource.isActive,
      })
    } else {
      form.resetFields()
    }
  }, [modalOpen, editingSource, form])

  const invalidateSources = () => queryClient.invalidateQueries({ queryKey: ['newsSources'] })

  const openCreateModal = () => {
    setEditingSource(null)
    setModalOpen(true)
  }

  const openEditModal = (source: NewsSource) => {
    setEditingSource(source)
    setModalOpen(true)
  }

  const onSubmit = (values: SourceFormValues) => {
    if (editingSource) {
      updateMutation.mutate(
        { id: editingSource.id, request: values },
        {
          onSuccess: () => {
            message.success('소스를 수정했어요.')
            setModalOpen(false)
            invalidateSources()
          },
          onError: (error) => message.error(getErrorMessage(error)),
        },
      )
      return
    }

    createMutation.mutate(values, {
      onSuccess: () => {
        message.success('소스를 추가했어요.')
        setModalOpen(false)
        invalidateSources()
      },
      onError: (error) => message.error(getErrorMessage(error)),
    })
  }

  const onToggleActive = (source: NewsSource, checked: boolean) => {
    toggleMutation.mutate(
      { id: source.id, request: { isActive: checked } },
      {
        onSuccess: () => invalidateSources(),
        // 스위치는 목록 데이터 기반이라 실패 시 자동으로 원래 상태를 유지한다.
        onError: (error) => message.error(getErrorMessage(error)),
      },
    )
  }

  const confirmDelete = (source: NewsSource) => {
    modal.confirm({
      title: '소스 삭제',
      content: `'${source.name}' 소스를 삭제할까요? 수집 이력은 유지됩니다.`,
      okText: '삭제',
      okButtonProps: { danger: true },
      cancelText: '취소',
      onOk: () =>
        deleteMutation.mutateAsync(source.id).then(
          () => {
            message.success('소스를 삭제했어요.')
            invalidateSources()
          },
          (error) => message.error(getErrorMessage(error)),
        ),
    })
  }

  const columns = [
    { title: 'ID', dataIndex: 'id', width: 70 },
    { title: '이름', dataIndex: 'name' },
    {
      title: 'URL',
      dataIndex: 'url',
      render: (url: string) => (
        <a href={url} target="_blank" rel="noreferrer">
          {url}
        </a>
      ),
    },
    {
      title: '활성',
      dataIndex: 'isActive',
      width: 80,
      render: (_: boolean, source: NewsSource) => (
        <Switch
          checked={source.isActive}
          aria-label={`${source.name} 활성`}
          loading={toggleMutation.isPending && toggleMutation.variables?.id === source.id}
          onChange={(checked) => onToggleActive(source, checked)}
        />
      ),
    },
    {
      title: '등록일',
      dataIndex: 'createdAt',
      width: 120,
      render: (createdAt: string) => createdAt.slice(0, 10),
    },
    {
      title: '',
      key: 'actions',
      width: 130,
      render: (_: unknown, source: NewsSource) => (
        <>
          <Button type="link" size="small" onClick={() => openEditModal(source)}>
            수정
          </Button>
          <Button type="link" size="small" danger onClick={() => confirmDelete(source)}>
            삭제
          </Button>
        </>
      ),
    },
  ]

  return (
    <div>
      <div className={styles.header}>
        <Typography.Title level={3} className={styles.title}>
          소스 관리
        </Typography.Title>
        <Button type="primary" icon={<PlusOutlined />} onClick={openCreateModal}>
          소스 추가
        </Button>
      </div>
      <Table
        rowKey="id"
        columns={columns}
        dataSource={sources}
        loading={isLoading}
        locale={{ emptyText: '등록된 소스가 없습니다' }}
        pagination={false}
      />
      <Modal
        title={editingSource ? '소스 수정' : '소스 추가'}
        open={modalOpen}
        onCancel={() => setModalOpen(false)}
        onOk={() => form.submit()}
        okText={editingSource ? '저장' : '추가'}
        cancelText="취소"
        confirmLoading={createMutation.isPending || updateMutation.isPending}
      >
        <Form form={form} layout="vertical" onFinish={onSubmit} initialValues={{ isActive: true }}>
          <Form.Item
            name="name"
            label="이름"
            rules={[
              { required: true, message: '이름을 입력해주세요' },
              { max: 100, message: '이름은 100자 이하로 입력해주세요' },
            ]}
          >
            <Input placeholder="소스 이름" />
          </Form.Item>
          <Form.Item
            name="url"
            label="URL"
            rules={[
              { required: true, message: 'URL을 입력해주세요' },
              { pattern: /^https?:\/\/.+/, message: 'http(s)로 시작하는 URL을 입력해주세요' },
              { max: 500, message: 'URL은 500자 이하로 입력해주세요' },
            ]}
          >
            <Input placeholder="https://example.com/feed" />
          </Form.Item>
          <Form.Item name="isActive" label="활성" valuePropName="checked">
            <Switch aria-label="활성 여부" />
          </Form.Item>
        </Form>
      </Modal>
    </div>
  )
}
