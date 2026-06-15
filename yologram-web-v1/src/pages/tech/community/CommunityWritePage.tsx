import { useState } from 'react'
import { useNavigate } from 'react-router'
import { useSetAtom } from 'jotai'
import { ArrowLeftOutlined } from '@ant-design/icons'
import { message } from 'antd'
import { techPostsAtom } from '../../../stores/techCommunity'
import type { CommunityPost } from '../../../types/techCommunity'
import { TECH_CATEGORIES, MAX_POST_CATEGORIES } from '../../../constants/techCategories'
import MultiSelectChips from '../../../components/common/MultiSelectChips'
import styles from './CommunityWritePage.module.css'

export default function CommunityWritePage() {
  const navigate = useNavigate()
  const setPosts = useSetAtom(techPostsAtom)

  const [title, setTitle] = useState('')
  const [content, setContent] = useState('')
  const [categories, setCategories] = useState<string[]>([])

  const canSubmit = content.trim().length > 0

  const toggleCategory = (item: string) => {
    setCategories((prev) => {
      if (prev.includes(item)) return prev.filter((c) => c !== item)
      if (prev.length >= MAX_POST_CATEGORIES) {
        message.warning(`카테고리는 최대 ${MAX_POST_CATEGORIES}개까지 선택할 수 있어요.`)
        return prev
      }
      return [...prev, item]
    })
  }

  const handleSubmit = () => {
    if (!canSubmit) return
    const newPost: CommunityPost = {
      id: Date.now(),
      board: 'TECH',
      author: '나',
      createdAt: '방금 전',
      title: title.trim() || undefined,
      content: content.trim(),
      categories,
      likeCount: 0,
      commentCount: 0,
      liked: false,
    }
    setPosts((prev) => [newPost, ...prev])
    message.success('글이 등록되었습니다.')
    navigate('/tech/community')
  }

  return (
    <div className={styles.container}>
      <div className={styles.header}>
        <button className={styles.back} aria-label="뒤로" onClick={() => navigate('/tech/community')}>
          <ArrowLeftOutlined />
        </button>
        <button className={styles.submit} disabled={!canSubmit} onClick={handleSubmit}>
          남기기
        </button>
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
        <div className={styles.categoryLabel}>카테고리 (최대 {MAX_POST_CATEGORIES}개)</div>
        <MultiSelectChips items={TECH_CATEGORIES} selected={categories} onToggle={toggleCategory} />
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
