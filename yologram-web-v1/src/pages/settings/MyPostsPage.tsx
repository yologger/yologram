import { useState } from 'react'
import { useNavigate } from 'react-router'
import { Typography } from 'antd'
import { ArrowLeftOutlined } from '@ant-design/icons'
import { useAtomValue } from 'jotai'
import { techPostsAtom } from '../../stores/techCommunity'
import type { CommunityBoard } from '../../types/techCommunity'
import FilterChips from '../../components/common/FilterChips'
import PostCard from '../tech/community/PostCard'
import styles from './MyPostsPage.module.css'

const { Title } = Typography

const BOARD_TABS: { label: string; board: CommunityBoard }[] = [
  { label: '기술', board: 'TECH' },
  { label: '투자', board: 'INVEST' },
  { label: '정치', board: 'POLITICS' },
]

export default function MyPostsPage() {
  const navigate = useNavigate()
  const posts = useAtomValue(techPostsAtom)
  const [label, setLabel] = useState('기술')

  const board = BOARD_TABS.find((t) => t.label === label)!.board
  const myPosts = posts.filter((p) => p.author === '나' && p.board === board)

  return (
    <div className={styles.container}>
      <div className={styles.header}>
        <button className={styles.back} aria-label="뒤로" onClick={() => navigate('/settings')}>
          <ArrowLeftOutlined />
        </button>
        <Title level={4} style={{ margin: 0 }}>내가 쓴 글</Title>
      </div>

      <div className={styles.body}>
        <FilterChips items={BOARD_TABS.map((t) => t.label)} selected={label} onChange={setLabel} />
        {myPosts.length === 0 ? (
          <div className={styles.empty}>작성한 글이 없어요</div>
        ) : (
          myPosts.map((post) => (
            <PostCard
              key={post.id}
              post={post}
              onClick={board === 'TECH' ? () => navigate(`/tech/community/${post.id}`) : undefined}
            />
          ))
        )}
      </div>
    </div>
  )
}
