# yologram-api-v1 할 일

앞으로 할 작업 체크리스트. 구현 완료된 기능·설계 근거는 features.md 참조.

## 인프라
- [ ] ECS 헬스체크 설정: actuator 의존성 추가 + Task Definition healthCheck

## DB 스키마 (RDS 직접 실행 — 엔티티는 구현됨, validate 모드라 DDL 수동)
- [ ] 신규 테이블 추가 시 DDL·인덱스 직접 실행 (현재 user/post/post_category/post_category_mapping 등은 적용 완료)

## UMS - Refresh Token
- [ ] refresh token 발급 (login 시 access + refresh 쌍)
- [ ] POST /api/v1/ums/auth/refresh (재발급)
- [ ] refresh token 저장/검증 로직
- [ ] 로그아웃 시 refresh token 폐기 (서버측 무효화 — access는 stateless라 refresh 도입과 함께)
- [ ] 테스트 / Swagger

## UMS - 회원탈퇴 soft delete 전환 (추후)
- [ ] status=DELETED + deletedDate, 탈퇴 유저 login/validate 차단(USER_WITHDRAWN 403)
- [ ] 유예기간 후 PII 익명화/하드삭제 배치, email 재가입 정책
- [ ] 연관 데이터 정리 비동기 처리(이벤트/큐) — 게시글 도메인 추가 후
- [ ] 조회 시 DELETED 유저 데이터 필터링
- 정리 전략 상세는 features.md "회원탈퇴 데이터 정리 전략" 참조

## UMS - 운영 보강 (이메일 인증·비밀번호 찾기 공통)
- [ ] 코드 해시 저장, 시도 횟수 제한/잠금
- [ ] 코드 발송 재발송 최소 간격 제한 (최근 발송 후 N초 이내 429)

## PMS - 게시글 수정/삭제
- [ ] PATCH /api/v1/pms/{section}/posts/{id} (본인 글 수정)
- [ ] DELETE /api/v1/pms/{section}/posts/{id} (본인 글 삭제)

## Comment - 댓글
- [ ] community_comments 테이블 (post_id FK 없이 인덱스 + app-level 검증)
- [ ] 댓글 작성/조회/삭제 API (/api/v1/comments/...)
  - 정렬 방식(최신순/오래된순), 대댓글 지원 여부는 구현 시 결정

## Count - 좋아요/카운트 (경로 예약)
- [ ] 좋아요 토글 (/api/v1/count/...)
  - 1차는 post 컬럼 동기 보관, 분리 시 이벤트 기반 카운트 이관

## QueryDSL 학습 예제 (legacy 이식, 토이/공부용)
- legacy BoardCustomRepository 기법: 멀티 leftJoin+on, Projections 중첩 DTO, coalesce, cursor/offset, count, id 범위
- 원칙: cross-domain join(닉네임/카운트)은 QueryClient로 회피. C는 학습 의도의 의식적 예외
- [ ] A. 내 글 목록(마이페이지): offset 페이지네이션 + count + 동적조건(BooleanBuilder) + projection. 피드 cursor와 대비 (legacy findBoardsWithMetricsByUid/countBoardsByUid 대응)
- [ ] B. 카테고리별 글 수 집계: post ↔ post_category_mapping join + groupBy + count
- [ ] C. 목록에 카테고리명 포함: post_category_mapping ↔ post_category join + Projections 중첩 (pms→cms 경계 결합, 학습용 예외)
- [ ] D. 벌크 update: 좋아요/조회수 증가 (count 도메인 도입 시)
- [ ] E. 기간별 글 조회: between/goe/loe 범위 조건

## Search - 검색 시스템 (추후 도입)
- [ ] 도입 시점 판단 (단순 목록은 pms cursor로, 검색·복잡 필터·대량 트래픽 필요 시 도입 — YAGNI)
- [ ] OpenSearch 인덱스 설계 (게시글 문서: section, 카테고리, 작성자, 본문, 카운트)
- [ ] 검색 인덱서(yologram-search-indexer): pms 변경 이벤트 → OpenSearch 동기화
- [ ] 검색 API(yologram-search-api): 키워드/카테고리/섹션 검색·필터·정렬·집계
- [ ] 프론트 이관: 공개 다건 탐색을 search로 (단건·쓰기·내 글은 pms 유지)
- CQRS·호출 기준 상세는 features.md "검색 시스템" 참조

## Admin - 카테고리 관리
- [ ] POST/DELETE/GET /api/v1/cms/admin/{section}/categories (어드민 권한)
- [ ] 카테고리 삭제 시 기존 글 처리 정책 (is_active vs 매핑 제거)

## Admin - 유저 관리
- [ ] GET /api/v1/ums/admin/users, GET /{uid}, PATCH /{uid}, DELETE /{uid}
- [ ] 어드민 권한 검증 (UserType.ADMIN)
- [ ] 테스트 / Swagger

## Admin - 게시글 관리
- [ ] GET /api/v1/admin/posts, GET /{id}, DELETE /{id}
- [ ] 테스트 / Swagger

## 보류/제외 (현재 범위 밖)
- [ ] OAuth 로그인 (Gmail, Kakao)
- [ ] 프로필 이미지 업로드
