# yologram 할 일 (통합)

> 전 프로젝트 통합 체크리스트. 구현 기능·설계 근거는 features.md.
> docs는 메인(루트) 에이전트만 갱신한다. 서브에이전트는 read-only.

---

## yologram-api-v1 할 일

앞으로 할 작업 체크리스트. 구현 완료된 기능·설계 근거는 features.md 참조.

### 인프라
- [ ] ECS 헬스체크 설정: actuator 의존성 추가 + Task Definition healthCheck

### DB 스키마 (RDS 직접 실행 — 엔티티는 구현됨, validate 모드라 DDL 수동)
- [ ] 신규 테이블 추가 시 DDL·인덱스 직접 실행 (현재 user/post/post_category/post_category_mapping 등은 적용 완료)

### UMS - Refresh Token
- [ ] refresh token 발급 (login 시 access + refresh 쌍)
- [ ] POST /api/v1/ums/auth/refresh (재발급)
- [ ] refresh token 저장/검증 로직
- [ ] 로그아웃 시 refresh token 폐기 (서버측 무효화 — access는 stateless라 refresh 도입과 함께)
- [ ] 테스트 / Swagger

### UMS - 회원탈퇴 soft delete 전환 (추후)
- [ ] status=DELETED + deletedDate, 탈퇴 유저 login/validate 차단(USER_WITHDRAWN 403)
- [ ] 유예기간 후 PII 익명화/하드삭제 배치, email 재가입 정책
- [ ] 연관 데이터 정리 비동기 처리(이벤트/큐) — 게시글 도메인 추가 후
- [ ] 조회 시 DELETED 유저 데이터 필터링
- 정리 전략 상세는 features.md "회원탈퇴 데이터 정리 전략" 참조

### UMS - 운영 보강 (이메일 인증·비밀번호 찾기 공통)
- [ ] 코드 해시 저장, 시도 횟수 제한/잠금
- [ ] 코드 발송 재발송 최소 간격 제한 (최근 발송 후 N초 이내 429)

### PMS - 게시글 수정/삭제
- [ ] PATCH /api/v1/pms/{section}/posts/{id} (본인 글 수정)
- [ ] DELETE /api/v1/pms/{section}/posts/{id} (본인 글 삭제)

### Comment - 댓글
- [ ] community_comments 테이블 (post_id FK 없이 인덱스 + app-level 검증)
- [ ] 댓글 작성/조회/삭제 API (/api/v1/comments/...)
  - 정렬 방식(최신순/오래된순), 대댓글 지원 여부는 구현 시 결정

### Count - 좋아요/카운트 (경로 예약)
- [ ] 좋아요 토글 (/api/v1/count/...)
  - 1차는 post 컬럼 동기 보관, 분리 시 이벤트 기반 카운트 이관

### QueryDSL 학습 예제 (legacy 이식, 토이/공부용)
- legacy BoardCustomRepository 기법: 멀티 leftJoin+on, Projections 중첩 DTO, coalesce, cursor/offset, count, id 범위
- 원칙: cross-domain join(닉네임/카운트)은 QueryClient로 회피. C는 학습 의도의 의식적 예외
- [ ] A. 내 글 목록(마이페이지): offset 페이지네이션 + count + 동적조건(BooleanBuilder) + projection. 피드 cursor와 대비 (legacy findBoardsWithMetricsByUid/countBoardsByUid 대응)
- [ ] B. 카테고리별 글 수 집계: post ↔ post_category_mapping join + groupBy + count
- [ ] C. 목록에 카테고리명 포함: post_category_mapping ↔ post_category join + Projections 중첩 (pms→cms 경계 결합, 학습용 예외)
- [ ] D. 벌크 update: 좋아요/조회수 증가 (count 도메인 도입 시)
- [ ] E. 기간별 글 조회: between/goe/loe 범위 조건

### Search - 검색 시스템 (추후 도입)
- [ ] 도입 시점 판단 (단순 목록은 pms cursor로, 검색·복잡 필터·대량 트래픽 필요 시 도입 — YAGNI)
- [ ] OpenSearch 인덱스 설계 (게시글 문서: section, 카테고리, 작성자, 본문, 카운트)
- [ ] 검색 인덱서(yologram-search-indexer): pms 변경 이벤트 → OpenSearch 동기화
- [ ] 검색 API(yologram-search-api): 키워드/카테고리/섹션 검색·필터·정렬·집계
- [ ] 프론트 이관: 공개 다건 탐색을 search로 (단건·쓰기·내 글은 pms 유지)
- CQRS·호출 기준 상세는 features.md "검색 시스템" 참조

### Admin - 카테고리 관리
- [ ] POST/DELETE/GET /api/v1/cms/admin/{section}/categories (어드민 권한)
- [ ] 카테고리 삭제 시 기존 글 처리 정책 (is_active vs 매핑 제거)

### Admin - 유저 관리
- [ ] GET /api/v1/ums/admin/users, GET /{uid}, PATCH /{uid}, DELETE /{uid}
- [ ] 어드민 권한 검증 (UserType.ADMIN)
- [ ] 테스트 / Swagger

### Admin - 게시글 관리
- [ ] GET /api/v1/admin/posts, GET /{id}, DELETE /{id}
- [ ] 테스트 / Swagger

### 보류/제외 (현재 범위 밖)
- [ ] OAuth 로그인 (Gmail, Kakao)
- [ ] 프로필 이미지 업로드

---

## yologram-api-v2 할 일

앞으로 할 작업 체크리스트. 구현 완료된 기능·설계 근거는 features.md 참조.

### 인프라
- [ ] GitHub Actions 빌드 캐시 적용: Docker 레이어 캐시 (docker/build-push-action)

### DB 스키마 (RDS 직접 실행 — 모델은 구현됨, DDL 수동)
- [ ] 신규 테이블 추가 시 DDL·인덱스 직접 실행
  - post / post_category_mapping 테이블은 api-v1과 공유 (DB 직접 실행 필요)
  - user/user_email_verification/user_password_reset_code/post_category 등은 적용 완료

### UMS - Refresh Token
- [ ] refresh token 발급 (login 시 access + refresh 쌍)
- [ ] POST /api/v2/ums/auth/refresh (재발급)
- [ ] refresh token 저장/검증 로직
- [ ] 로그아웃 시 refresh token 폐기 (서버측 무효화 — access는 stateless라 refresh 도입과 함께)
- [ ] 테스트 / Swagger

### UMS - 회원탈퇴 soft delete 전환 (추후)
- [ ] status=DELETED + deleted_date, 탈퇴 유저 login/validate 차단(USER_WITHDRAWN 403)
- [ ] 유예기간 후 PII 익명화/하드삭제 배치, email 재가입 정책
- [ ] 연관 데이터 정리 비동기 처리(이벤트/큐) — 게시글 도메인 추가 후
- [ ] 조회 시 DELETED 유저 데이터 필터링
- 정리 전략 상세는 features.md "회원탈퇴 데이터 정리 전략" 참조

### UMS - 운영 보강 (이메일 인증·비밀번호 찾기 공통)
- [ ] 코드 해시 저장, 시도 횟수 제한/잠금
- [ ] 코드 발송 재발송 최소 간격 제한 (최근 발송 후 N초 이내 429)

### PMS - 게시글 수정/삭제
- [ ] PATCH /api/v2/pms/{section}/posts/{id} (본인 글 수정)
- [ ] DELETE /api/v2/pms/{section}/posts/{id} (본인 글 삭제)

### Comment - 댓글
- [ ] community_comments 테이블 (post_id FK 없이 인덱스 + app-level 검증)
- [ ] 댓글 작성/조회/삭제 API (/api/v2/comments/...)
  - 정렬 방식(최신순/오래된순), 대댓글 지원 여부는 구현 시 결정

### Count - 좋아요/카운트 (경로 예약)
- [ ] 좋아요 토글 (/api/v2/count/...)
  - 1차는 post 컬럼 동기 보관, 분리 시 이벤트 기반 카운트 이관

### Search - 검색 시스템 (추후 도입)
- [ ] 도입 시점 판단 (단순 목록은 pms cursor로, 검색·복잡 필터·대량 트래픽 필요 시 도입 — YAGNI)
- [ ] OpenSearch 인덱스 설계 (게시글 문서: section, 카테고리, 작성자, 본문, 카운트)
- [ ] 검색 인덱서(yologram-search-indexer): pms 변경 이벤트 → OpenSearch 동기화
- [ ] 검색 API(yologram-search-api): 키워드/카테고리/섹션 검색·필터·정렬·집계
- [ ] 프론트 이관: 공개 다건 탐색을 search로 (단건·쓰기·내 글은 pms 유지)
- CQRS·호출 기준 상세는 features.md "검색 시스템" 참조

### Admin - 카테고리 관리
- [ ] POST/DELETE/GET /api/v2/cms/admin/{section}/categories (어드민 권한)
- [ ] 카테고리 삭제 시 기존 글 처리 정책 (is_active vs 매핑 제거)

### Admin - 유저 관리
- [ ] GET /api/v2/ums/admin/users, GET /{uid}, PATCH /{uid}, DELETE /{uid}
- [ ] 어드민 권한 검증 (UserType.ADMIN)
- [ ] 테스트 / Swagger

### Admin - 게시글 관리
- [ ] GET /api/v2/admin/posts, GET /{id}, DELETE /{id}
- [ ] 테스트 / Swagger

### 보류/제외 (현재 범위 밖)
- [ ] OAuth 로그인 (Gmail, Kakao)
- [ ] 프로필 이미지 업로드

---

## yologram-web-v1 할 일

앞으로 할 작업 체크리스트. 구현 완료된 화면·기능·설계 근거는 features.md 참조.

### 기술 커뮤니티 (추후 확장)
- [ ] invest/politics 피드 연동
- [ ] 인증 게이팅
- [ ] 댓글 무한스크롤
- [ ] 팔로우/리포스트/공유/이모지/정렬/작성 툴바 동작

### 공통 기능
- [ ] 다크모드 지원 (Ant Design theme + 사용자 설정 저장)
- [ ] 코드 발송 버튼 재발송 쿨다운 (localStorage 기반, 새로고침 유지, forgot-password·join 공통)

### Refresh Token
- [ ] login 응답에서 refresh token 저장
- [ ] 401 시 refresh token으로 access token 재발급 후 재요청

### 설정 - 환경 설정
- [ ] 알림 설정 페이지
- [ ] 다크 모드 설정 페이지

### 설정 - 활동
- [ ] 저장한 글 페이지
- [ ] 내가 쓴 글: 현재 더미 → 내 글 목록 API 연동

### 보류/제외 (현재 범위 밖)
- [ ] OAuth 로그인 (Gmail, Kakao)
- [ ] 프로필 이미지 업로드

---

## yologram-web-v2 할 일

앞으로 할 작업 체크리스트. 구현 완료된 기능·설계 근거는 features.md 참조.

### 공통 기능
- [ ] 코드 발송 버튼 재발송 쿨다운 (localStorage 기반, 새로고침 유지, forgot-password·join 공통)

### 기술 커뮤니티 (추후 확장)
- [ ] invest/politics 피드 연동
- [ ] 내 글 목록 API 연동
- [ ] 팔로우/리포스트/공유/이모지/정렬/작성 툴바 동작

### Refresh Token
- [ ] login 응답에서 refresh token 저장
- [ ] 401 시 refresh token으로 access token 재발급 후 재요청

### 설정 - 환경 설정
- [ ] 알림 설정 페이지
- [ ] 다크 모드 설정 페이지

### 설정 - 활동
- [ ] 내가 쓴 글 페이지
- [ ] 저장한 글 페이지

### Observability 운영
- [ ] staging/prod 런타임에 OTEL secret 주입
- [ ] Grafana Tempo에서 trace 수신 확인
- [ ] GitHub Actions 빌드 캐시 적용

### Next.js / 인증 구조 학습·검토
- [ ] Next.js 서버 컴포넌트 / 클라이언트 컴포넌트 완벽히 이해하기
- [ ] 인증: cookie 기반 토큰 전환 시 middleware 방식으로 route 보호 검토
- [ ] route group 구조 검토

### 보류/제외 (현재 범위 밖)

#### Observability
- [ ] browser RUM
- [ ] client-side tracing
- [ ] logs
- [ ] custom metrics (이번 단계에서 추가하지 않음)
- 향후 개선 선택지: Grafana Alloy + spanmetrics로 trace에서 request 메트릭 추출, access log 보강, 최소 custom metric 추가
