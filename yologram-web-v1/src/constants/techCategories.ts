// 글 작성/배지/시드에 사용하는 실제 카테고리
export const TECH_CATEGORIES = ['Frontend', 'Backend', 'AI/ML', 'DevOps', 'Cloud', 'Security', '기타']

// 필터 전용 "전체"(모든 글 보기)
export const ALL_CATEGORY = '전체'

// 뉴스/커뮤니티 필터에 표시하는 목록 (전체 + 실제 카테고리)
export const TECH_FILTER_CATEGORIES = [ALL_CATEGORY, ...TECH_CATEGORIES]

// 커뮤니티 글 작성 시 선택 가능한 최대 카테고리 수
export const MAX_POST_CATEGORIES = 3
