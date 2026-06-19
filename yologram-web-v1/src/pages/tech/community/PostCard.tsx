import { Avatar } from 'antd'
import { UserOutlined, HeartOutlined, MessageOutlined } from '@ant-design/icons'
import type { CommunityPost } from '../../../types/techCommunity'
import styles from './PostCard.module.css'

interface Props {
  post: CommunityPost
  categoryNames?: string[]
  onClick?: () => void
}

export default function PostCard({ post, categoryNames = [], onClick }: Props) {
  return (
    <div className={styles.card} onClick={onClick}>
      <div className={styles.head}>
        <Avatar size={32} icon={<UserOutlined />} />
        <span className={styles.author}>{post.author}</span>
        <span className={styles.time}>{post.createdAt}</span>
      </div>
      {post.title && <div className={styles.title}>{post.title}</div>}
      <div className={styles.content}>{post.content}</div>
      {categoryNames.length > 0 && (
        <div className={styles.badges}>
          {categoryNames.map((c) => (
            <span key={c} className={styles.badge}>{c}</span>
          ))}
        </div>
      )}
      <div className={styles.meta}>
        <span><HeartOutlined /> {post.likeCount}</span>
        <span><MessageOutlined /> {post.commentCount}</span>
      </div>
    </div>
  )
}
