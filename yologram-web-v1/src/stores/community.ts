import { atom } from 'jotai'
import type { CommunityComment } from '../types/community'

function seedComments(): CommunityComment[] {
  return [
    {
      id: 1,
      postId: 1000,
      author: '활발한고양이303',
      createdAt: '6시간 전',
      content: '그래도 반도체는 사이클이 있어서 저는 둘 다 모으는 중',
      likeCount: 2,
    },
  ]
}

export const communityCommentsAtom = atom<CommunityComment[]>(seedComments())
