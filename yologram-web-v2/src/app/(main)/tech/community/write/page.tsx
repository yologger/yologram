'use client'

import { useState } from 'react'
import { useRouter } from 'next/navigation'
import { useQueryClient } from '@tanstack/react-query'
import { ArrowLeftOutlined } from '@ant-design/icons'
import { App, Button } from 'antd'
import { MAX_POST_CATEGORIES } from '@/constants/community'
import MultiSelectChips from '@/components/common/MultiSelectChips'
import { type ChipItem } from '@/components/common/FilterChips'
import usePostCategoriesQuery from '@/queries/usePostCategoriesQuery'
import useCreatePostMutation from '@/queries/useCreatePostMutation'
import RequireAuth from '@/components/auth/RequireAuth'
import styles from './CommunityWrite.module.css'

export default function CommunityWrite() {
  const router = useRouter()
  const { message } = App.useApp()
  const queryClient = useQueryClient()
  const { data: categories = [] } = usePostCategoriesQuery('tech')
  const { mutate: createPost, isPending } = useCreatePostMutation()

  const [title, setTitle] = useState('')
  const [content, setContent] = useState('')
  const [categoryIds, setCategoryIds] = useState<number[]>([])

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

    const trimmedTitle = title.trim() || undefined
    const trimmedContent = content.trim()

    createPost(
      { section: 'tech', request: { title: trimmedTitle, content: trimmedContent, categoryIds } },
      {
        onSuccess: () => {
          // 피드 목록 무효화 → 최신순 재조회 시 새 글이 맨 위에 노출
          queryClient.invalidateQueries({ queryKey: ['posts', 'tech'] })
          message.success('글이 등록되었습니다.')
          router.push('/tech/community')
        },
        onError: () => {
          message.error('글 등록에 실패했어요. 잠시 후 다시 시도해주세요.')
        },
      },
    )
  }

  return (
    <RequireAuth>
      <div className={styles.container}>
        <div className={styles.header}>
          <button className={styles.back} aria-label="뒤로" onClick={() => router.push('/tech/community')}>
            <ArrowLeftOutlined />
          </button>
          <Button
            type="link"
            className={styles.submit}
            loading={isPending}
            disabled={!canSubmit}
            onClick={handleSubmit}
          >
            남기기
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
    </RequireAuth>
  )
}
