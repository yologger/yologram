'use client'

import { useState } from 'react'
import { ArrowLeftOutlined } from '@ant-design/icons'
import { App, Button } from 'antd'
import { MAX_POST_CATEGORIES } from '@/constants/community'
import MultiSelectChips from '@/components/common/MultiSelectChips'
import { type ChipItem } from '@/components/common/FilterChips'
import usePostCategoriesQuery from '@/queries/usePostCategoriesQuery'
import styles from '@/app/(main)/tech/community/write/CommunityWrite.module.css'

export interface CommunityPostFormValues {
  title: string
  content: string
  categoryIds: number[]
}

interface CommunityPostFormProps {
  section: string
  // 제출 버튼 라벨 (작성: 남기기 / 수정: 수정)
  submitLabel: string
  // prefill 초기값 (수정 모드)
  initialValues?: CommunityPostFormValues
  isSubmitting: boolean
  onCancel: () => void
  onSubmit: (values: { title?: string; content: string; categoryIds: number[] }) => void
}

export default function CommunityPostForm({
  section,
  submitLabel,
  initialValues,
  isSubmitting,
  onCancel,
  onSubmit,
}: CommunityPostFormProps) {
  const { message } = App.useApp()
  const { data: categories = [] } = usePostCategoriesQuery(section)

  const [title, setTitle] = useState(initialValues?.title ?? '')
  const [content, setContent] = useState(initialValues?.content ?? '')
  const [categoryIds, setCategoryIds] = useState<number[]>(initialValues?.categoryIds ?? [])

  const canSubmit = content.trim().length > 0 && categoryIds.length > 0

  const categoryItems: Array<ChipItem<number>> = categories.map((c) => ({ label: c.name, value: c.id }))

  const toggleCategory = (id: number) => {
    setCategoryIds((prev) => {
      if (prev.includes(id)) return prev.filter((c) => c !== id)
      if (prev.length >= MAX_POST_CATEGORIES) {
        message.warning(`카테고리는 최대 ${MAX_POST_CATEGORIES}개까지 선택할 수 있어요.`)
        return prev
      }
      return [...prev, id]
    })
  }

  const handleSubmit = () => {
    if (!canSubmit) return
    onSubmit({ title: title.trim() || undefined, content: content.trim(), categoryIds })
  }

  return (
    <div className={styles.container}>
      <div className={styles.header}>
        <button className={styles.back} aria-label="뒤로" onClick={onCancel}>
          <ArrowLeftOutlined />
        </button>
        <Button
          type="link"
          className={styles.submit}
          loading={isSubmitting}
          disabled={!canSubmit}
          onClick={handleSubmit}
        >
          {submitLabel}
        </Button>
      </div>

      <div className={styles.body}>
        <input
          className={styles.titleInput}
          placeholder="제목을 입력해주세요 (선택)"
          value={title}
          onChange={(e) => setTitle(e.target.value)}
        />
        <textarea
          className={styles.contentInput}
          placeholder="광고, 비난, 도배성 글을 남기면 활동이 제한될 수 있어요. 건강한 커뮤니티 문화를 함께 만들어가요."
          value={content}
          onChange={(e) => setContent(e.target.value)}
        />
        <div className={styles.categoryLabel}>카테고리 (1~{MAX_POST_CATEGORIES}개 선택)</div>
        <MultiSelectChips items={categoryItems} selected={categoryIds} onToggle={toggleCategory} />
      </div>

      <div className={styles.toolbar}>
        <span className={styles.toolbarItem}>GIF</span>
        <span className={styles.toolbarItem}>사진</span>
        <span className={styles.toolbarItem}>투표</span>
        <span className={styles.toolbarItem}>태그</span>
      </div>
    </div>
  )
}
