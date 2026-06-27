# 구현 기능, 설계 근거

> 전 프로젝트(api-v1/api-v2/web-v1/web-v2)의 구현 기능과 설계 근거를 한 곳에서 관리. 앞으로 할 일은 todos.md.
> 백엔드는 api-v1(Spring Boot/Kotlin)·api-v2(FastAPI/Python) 미러링, 프론트는 web-v1(React)·web-v2(Next.js) 미러링. 별도 표기 없으면 각 계층 양쪽 공통.
> api-v2는 api-v1을 FastAPI로 미러링한 비교학습용(객체지향·Layered·DI 적용), web-v1은 web-v2와 동일 기능의 React 비교학습용.
> docs/는 메인(루트) 에이전트만 갱신. 서브에이전트는 read-only(참고만).

---

## 구현 완료
- [x] (Observability) Grafana Cloud OTLP direct push (api-v1/v2 logs·metrics·traces, web-v2 server-side trace + metrics)
- [x] (CI/CD) GitHub Actions(ECR push → ECS 재배포), Discord 알림(env + jq로 셸 인젝션 회피)
- [x] (UMS) 회원가입 + 이메일 인증
  - [x] AWS SES를 통한 이메일 발송
  - 6자리 코드(5분 유효) 인증 후 가입. EmailSender로 발송 추상화(개발 Stub 로그 / 운영 SES). user_email_verification 테이블, 가입 후 코드 삭제
- [x] (UMS) 로그인
  - JWT(HMAC256) access token 발급. 동일 secret/issuer/audience를 api-v1·v2 공유
- [x] (UMS) access token 발급, 검증
  - access token은 stateless(서버 미저장). validate-token은 로그인 직후 replica lag 회피 위해 master DB 조회(api-v1)
  - [x] 프론트엔드 AuthGate가 앱 시작/마운트 시 저장 토큰 검증 후 렌더링
- [x] (UMS) 로그아웃
  - stateless라 클라이언트 토큰 폐기 방식. 다중 로그인 지원 위해 DB 토큰 비교 없음. 서버측 강제 무효화는 refresh token 도입 시
- [x] (UMS) 비밀번호 변경
  - PATCH /ums/user/me/password (현재 비밀번호 확인 후 변경)
- [x] (UMS) 회원정보 조회
  - GET /ums/user/me
- [x] (UMS) 회원정보 수정
  - PATCH /ums/user/me (닉네임 변경, 이메일·이름은 읽기전용)
- [x] (UMS) 비밀번호 찾기
  - [x] AWS SES를 통한 이메일 발송
  - 6자리 코드(5분 유효) 발송→검증→재설정. user_password_reset_code 테이블(회원가입 인증과 분리). 미가입 이메일 발송 시 404
- [x] (UMS) 회원탈퇴
  - 현재 개발 단계 하드 삭제(email 즉시 해제되어 재가입 가능). 추후 soft delete 전환 예정(정리 전략은 todos.md 참조)
- [x] (CMS) 커뮤니티 카테고리 조회
  - [x] 백엔드: GET /cms/{section}/categories (is_active=true, sort_order 정렬)
  - [x] 프론트: 섹션별 필터 칩 동적 렌더(전체 + 카테고리), 단일선택 필터 / 작성 시 다중 태깅(최대 3)
  - 카테고리는 cms 소유의 어드민 관리 메타데이터(post_category 테이블), 코드 상수가 아닌 DB로 관리
- [x] (PMS) 게시글 작성
  - POST /pms/{section}/posts (인증). 작성자=인증 유저, categoryIds 1~3개 검증. 요청 { title?, content, categoryIds[] }
- [x] (PMS) 게시글 상세 조회
  - GET /pms/{section}/posts/{id} (공개). author{uid, nickname}는 UserQueryClient로 ums 조회. 없거나 다른 section이면 404
- [x] (PMS) 게시글 다건 조회
  - [x] 백엔드 Cursor-based Pagination (id-only 커서, 마지막 글 id를 nextCursor로·빈 결과면 null. +1/hasNext/count 미사용)
  - [x] 프론트: cursor 무한스크롤(useInfiniteQuery, nextCursor 기준)
- [x] (Settings, PMS)
- [x] 내가 쓴 글
  - /settings/my-posts (현재 더미 → 내 글 목록 API 연동 예정)
