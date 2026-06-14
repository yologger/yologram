export interface CommunityPost {
  id: number
  author: string
  createdAt: string
  title?: string
  content: string
  categories: string[]
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
