import { atom } from 'jotai'
import type { CommunityPost, CommunityComment } from '../types/community'

// "내가 쓴 글" 데모용 (author='나', 게시판별). 내 글 목록 API 도입 전까지 임시
// categoryIds는 로컬 시드 기준 (TECH 1~7, INVEST 8~13, POLITICS 14~18), createdAt은 ISO
function seedPosts(): CommunityPost[] {
  return [
    { id: 2001, section: 'TECH', author: '나', createdAt: '2026-06-18T09:00:00', content: 'Next.js App Router 전환 후기 공유합니다', categoryIds: [1], likeCount: 3, commentCount: 1, liked: false },
    { id: 2002, section: 'INVEST', author: '나', createdAt: '2026-06-17T09:00:00', content: '해외주식 분할매수 전략 어떻게들 하시나요', categoryIds: [9], likeCount: 5, commentCount: 2, liked: false },
    { id: 2003, section: 'POLITICS', author: '나', createdAt: '2026-06-16T09:00:00', content: '이번 정책 토론 정리해봤습니다', categoryIds: [16], likeCount: 1, commentCount: 0, liked: false },
  ]
}

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

export const communityPostsAtom = atom<CommunityPost[]>(seedPosts())
export const communityCommentsAtom = atom<CommunityComment[]>(seedComments())
