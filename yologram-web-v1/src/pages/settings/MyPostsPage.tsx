import { useMemo, useState } from 'react'
import { useNavigate } from 'react-router'
import { Typography } from 'antd'
import { ArrowLeftOutlined } from '@ant-design/icons'
import { useAtomValue } from 'jotai'
import { communityPostsAtom } from '../../stores/community'
import type { CommunitySection } from '../../types/community'
import FilterChips from '../../components/common/FilterChips'
import PostCard from '../tech/community/PostCard'
import useCategoriesQuery from '../../queries/useCategoriesQuery'
import styles from './MyPostsPage.module.css'

const { Title } = Typography

const SECTION_TABS: { label: string; section: CommunitySection }[] = [
  { label: '기술', section: 'TECH' },
  { label: '투자', section: 'INVEST' },
  { label: '정치', section: 'POLITICS' },
]

export default function MyPostsPage() {
  const navigate = useNavigate()
  const posts = useAtomValue(communityPostsAtom)
  const [label, setLabel] = useState('기술')

  const section = SECTION_TABS.find((t) => t.label === label)!.section
  const myPosts = posts.filter((p) => p.author === '나' && p.section === section)

  const { data: categories = [] } = useCategoriesQuery(section.toLowerCase())
  const nameById = useMemo(() => new Map(categories.map((c) => [c.id, c.name])), [categories])

  return (
    <div className={styles.container}>
      <div className={styles.header}>
        <button className={styles.back} aria-label="뒤로" onClick={() => navigate('/settings')}>
          <ArrowLeftOutlined />
        </button>
        <Title level={4} style={{ margin: 0 }}>내가 쓴 글</Title>
      </div>

      <div className={styles.body}>
        <FilterChips items={SECTION_TABS.map((t) => t.label)} selected={label} onChange={setLabel} />
        {myPosts.length === 0 ? (
          <div className={styles.empty}>작성한 글이 없어요</div>
        ) : (
          myPosts.map((post) => (
            <PostCard
              key={post.id}
              post={post}
              categoryNames={post.categoryIds.map((id) => nameById.get(id)).filter((n): n is string => !!n)}
              onClick={section === 'TECH' ? () => navigate(`/tech/community/${post.id}`) : undefined}
            />
          ))
        )}
      </div>
    </div>
  )
}
