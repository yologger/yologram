import { atom } from 'jotai'
import type { CommunityPost, CommunityComment } from '../types/community'

// 로컬 시드 기준 TECH 카테고리 id (categories 테이블 1~7)
const TECH_CATEGORY_IDS = [1, 2, 3, 4, 5, 6, 7]

const NICKNAMES = [
  '따뜻한카나리아210', '활발한고양이303', '느긋한판다88', '용감한여우17',
  '조용한수달42', '명랑한북극곰9', '엉뚱한미어캣77', '성실한두더지5',
]

const CONTENTS = [
  'qld보다 이게 더 좋나요 음의복리도 없고 수익률도 더 높던데',
  '반도체는 사이클이 있어서 저는 둘 다 모으는 중',
  '오늘 장 분위기 어떤가요? 추가 매수 고민됩니다',
  '엔비디아 실적 발표 다들 기대하시나요',
  'AI 관련주 비중 어느 정도로 가져가세요?',
  '장기투자 관점에서 지금이 기회인 것 같습니다',
  '환율 때문에 고민이네요 분할매수가 답일까요',
  '기술주 조정 올 때마다 줍줍 중입니다',
]

function seedCategoryIds(i: number): number[] {
  const start = i % TECH_CATEGORY_IDS.length
  const count = (i % 3) + 1
  return Array.from({ length: count }, (_, k) => TECH_CATEGORY_IDS[(start + k) % TECH_CATEGORY_IDS.length])
}

function seedPosts(): CommunityPost[] {
  const posts: CommunityPost[] = Array.from({ length: 60 }, (_, i) => ({
    id: 1000 - i,
    section: 'TECH' as const,
    author: NICKNAMES[i % NICKNAMES.length],
    createdAt: `${(i % 23) + 1}시간 전`,
    content: CONTENTS[i % CONTENTS.length],
    categoryIds: seedCategoryIds(i),
    likeCount: (i * 7) % 50,
    commentCount: (i * 3) % 12,
    liked: false,
  }))

  // "내가 쓴 글" 데모용 (author='나', 게시판별)
  // categoryIds는 로컬 시드 기준 (TECH 1~7, INVEST 8~13, POLITICS 14~18)
  const mine: CommunityPost[] = [
    { id: 2001, section: 'TECH', author: '나', createdAt: '1일 전', content: 'Next.js App Router 전환 후기 공유합니다', categoryIds: [1], likeCount: 3, commentCount: 1, liked: false },
    { id: 2002, section: 'INVEST', author: '나', createdAt: '2일 전', content: '해외주식 분할매수 전략 어떻게들 하시나요', categoryIds: [9], likeCount: 5, commentCount: 2, liked: false },
    { id: 2003, section: 'POLITICS', author: '나', createdAt: '3일 전', content: '이번 정책 토론 정리해봤습니다', categoryIds: [16], likeCount: 1, commentCount: 0, liked: false },
  ]

  return [...posts, ...mine]
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
