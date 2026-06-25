# yologram-web-v1 할 일

앞으로 할 작업 체크리스트. 구현 완료된 화면·기능·설계 근거는 features.md 참조.

## 기술 커뮤니티 (UI, 더미데이터)
- [ ] 게시글/댓글 타입 + Jotai atom (더미 시드)
- [ ] 피드 페이지(/tech/community): PostCard 목록 + 무한 스크롤 + 하단 작성바
- [ ] 글 작성 페이지(/tech/community/write): 제목(optional)+내용, 안내문구, 하단 툴바 플레이스홀더
- [ ] 글 상세 페이지(/tech/community/:postId): 본문 + 액션행 + 댓글 목록 + 댓글 입력바
- [ ] 좋아요 로컬 토글, 작성/댓글 제출 시 atom 추가
- [ ] 기술 서브탭 헤더 스크롤 시 숨김/표시 (SubTabLayout collapseOnScroll, useScrollDirection)
- [ ] 우하단 맨 위로 가기 FAB (ScrollToTopButton)
- [ ] 테스트
- [ ] (추후) invest/politics 피드 연동, 내 글 목록 API, 인증 게이팅, 팔로우/리포스트/공유/이모지/정렬/작성 툴바 동작, 댓글 무한스크롤

## 공통 기능
- [ ] 다크모드 지원 (Ant Design theme + 사용자 설정 저장)
- [ ] 코드 발송 버튼 재발송 쿨다운 (localStorage 기반, 새로고침 유지, forgot-password·join 공통)

## Refresh Token
- [ ] login 응답에서 refresh token 저장
- [ ] 401 시 refresh token으로 access token 재발급 후 재요청

## 설정 - 환경 설정
- [ ] 알림 설정 페이지
- [ ] 다크 모드 설정 페이지

## 설정 - 활동
- [ ] 저장한 글 페이지
- [ ] 내가 쓴 글: 현재 더미 → 내 글 목록 API 연동

## 보류/제외 (현재 범위 밖)
- [ ] OAuth 로그인 (Gmail, Kakao)
- [ ] 프로필 이미지 업로드
