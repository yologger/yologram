export type CommunitySection = 'TECH' | 'INVEST' | 'POLITICS'

export interface CommunityPost {
  id: number
  section: CommunitySection
  author: string
  createdAt: string
  title?: string
  content: string
  categoryIds: number[]
  likeCount: number
  commentCount: number
  liked: boolean
}

export interface CommunityComment {
  id: number
  postId: number
  author: string
  createdAt: string
  content: string
  likeCount: number
}
