# 해야할 작업

> 위에서부터 우선순위 순(급한 것이 위). 순서는 줄 이동으로 재조정한다.
> 한 줄 = 한 기능. 미러링되는 기능은 프로젝트별 하위 체크, 단일 프로젝트는 줄에 표기.
> 도메인 태그 `(UMS)` 등은 식별용(선택). 구현 기능·설계 근거는 features.md.
> docs/는 메인(루트) 에이전트만 갱신. 서브에이전트는 read-only(참고만).

## Todos. 
- [ ] (PMS) 게시글 수정/삭제 (본인 글)
  - [ ] 내 글에만 수정/삭제 버튼 활성화 (작성자=인증 유저일 때만 노출)
  - [ ] 게시글 수정 — api-v1/v2 PATCH /pms/{section}/posts/{id}, web-v1/v2
  - [ ] 게시글 삭제 — api-v1/v2 DELETE /pms/{section}/posts/{id}, web-v1/v2 (상세 + 내 글 목록, 확인 모달 → 삭제 후 invalidate)
- [ ] (마이페이지) 내 글 목록
  - [ ] api-v1 (QueryDSL offset 페이지네이션 + count + 동적조건 — 학습 예제 A 겸)
  - [ ] api-v2
  - [ ] web-v1 / web-v2 (현재 더미 → 연동)
- [ ] (Comment) 댓글
  - [ ] 댓글 작성 — community_comments 테이블(post_id FK 없이 인덱스 + app-level 검증, /comments/...), api-v1/v2, web-v1/v2
  - [ ] 댓글 조회 — 최신순/오래된순 정렬, web 무한스크롤 (상세 페이지 더미 → 연동)
  - [ ] 내 댓글에만 수정/삭제 버튼 활성화 (작성자=인증 유저일 때만 노출)
  - [ ] 댓글 수정 — api-v1/v2, web-v1/v2
  - [ ] 댓글 삭제 — api-v1/v2, web-v1/v2
  - 대댓글 지원 여부는 구현 시 결정
- [ ] (Count) 좋아요 토글 (/count 경로)
  - [ ] api-v1
  - [ ] api-v2
  - [ ] web-v1 / web-v2 (로컬 임시 토글 → 연동)
  - [ ] 좋아요 수 / 댓글 수 조회·표시 (게시글 목록·상세 카운트, api-v1/v2 + web)
  - 1차는 post 컬럼 동기 보관, 분리 시 이벤트 기반 카운트 이관
- [ ] (web) invest/politics 피드 연동
  - [ ] web-v1
  - [ ] web-v2
- [ ] (UMS) Refresh Token
  - [ ] api-v1 / api-v2 (발급·POST /ums/auth/refresh 재발급·저장/검증·로그아웃 시 폐기·테스트/Swagger)
  - [ ] web-v1 / web-v2 (login 시 저장, 401 시 재발급 후 재요청)
- [ ] (UMS/Admin) 회원 관리
  - [ ] api-v1 / api-v2 (GET /ums/admin/users, GET/PATCH/DELETE /{uid}, ADMIN 권한, 테스트/Swagger)
  - [ ] web-admin (회원정보 조회 등)
- [ ] (CMS/Admin) 카테고리 관리
  - [ ] api-v1 / api-v2 (POST/DELETE/GET /cms/admin/{section}/categories)
  - 카테고리 삭제 시 기존 글 처리 정책(is_active 비활성 vs 매핑 제거)
- [ ] (PMS/Admin) 게시글 관리
  - [ ] api-v1 / api-v2 (GET /admin/posts, GET/DELETE /{id})
- [ ] (web) 설정 — 알림 설정 페이지 (web-v1 / web-v2)
- [ ] (web) 설정 — 다크 모드 (web-v1 theme + 설정 저장, web-v1/v2 설정 페이지)
- [ ] (web) 설정 — 저장한 글 페이지 (web-v1 / web-v2)
- [ ] (web) 커뮤니티 확장: 팔로우/리포스트/공유/이모지/정렬/작성 툴바 (web-v1 / web-v2)
- [ ] (web-v1) 인증 게이팅
- [ ] (UMS) 운영 보강 — 이메일 인증·비밀번호 찾기 (api-v1/v2 공통)
  - [ ] 코드 해시 저장, 시도 횟수 제한/잠금
  - [ ] 발송 재발송 최소 간격 제한 (최근 발송 후 N초 이내 429)
  - [ ] web 코드 발송 버튼 재발송 쿨다운 (web-v1/v2, localStorage)
- [ ] (UMS) 회원탈퇴 soft delete 전환
  - [ ] api-v1 / api-v2 (status=DELETED + deletedDate, 탈퇴 유저 login/validate 차단 USER_WITHDRAWN 403)
  - [ ] 유예기간 후 PII 익명화/하드삭제 배치, email 재가입 정책
  - [ ] 연관 데이터 정리 비동기(이벤트/큐) — 게시글 도메인 추가 후
  - [ ] 조회 시 DELETED 유저 데이터 필터링
  - 데이터 정리 전략 (soft delete 전환 시 결정):
    - 현재(개발 단계): 레코드 즉시 하드 삭제(UserService.withdraw) → email 해제로 재가입 가능
    - 탈퇴 요청과 연관 데이터(게시글/댓글/좋아요) 삭제를 분리해 요청 부하 완화
    - 1차(동기, 즉시 응답): status=DELETED + deletedDate 기록, 토큰 무효화 후 204. 조회 시 DELETED 필터링 → 즉시 탈퇴 효과
    - 2차(비동기 연관 삭제) 옵션:
      - SQS 이벤트 + 워커 (권장, 확장성): "user-deleted" 발행 → 워커가 도메인별 배치 삭제, 실패 시 DLQ 재시도
      - 배치 잡 (간단): DELETED 유저를 주기 스캔해 청크 삭제, 별도 인프라 최소
      - 앱 내 @Async/BackgroundTasks (소규모/임시): 요청과 분리하나 인스턴스 종속 → 배포/장애 시 유실 위험, 대량 부적합
    - 대량 삭제는 청크(LIMIT N) 단위 반복으로 락/replica 지연 완화. 보관 의무 데이터는 익명화·유예기간(복구) 검토(soft delete면 자연 지원)
    - 권장: soft delete 즉시 응답 + SQS 워커 도메인별 청크 삭제 (규모 작으면 배치 잡으로 시작)
- [ ] (Search) 검색 시스템 (추후 도입, YAGNI — 검색·복잡 필터·대량 트래픽 필요 시)
  - [ ] 도입 시점 판단
  - [ ] OpenSearch 인덱스 설계 (게시글 문서: section, 카테고리, 작성자, 본문, 카운트)
  - [ ] 검색 인덱서(yologram-search-indexer): pms 변경 이벤트 → OpenSearch 동기화
  - [ ] 검색 API(yologram-search-api): 키워드/카테고리/섹션 검색·필터·정렬·집계
  - [ ] 프론트 이관: 공개 다건 탐색 → search (단건·쓰기·내 글은 pms 유지)
  - CQRS·호출 기준은 features.md 참조
- [ ] (api-v1) QueryDSL 학습 예제 (legacy 이식, 토이/공부용)
  - [ ] B. 카테고리별 글 수 집계: post ↔ post_category_mapping join + groupBy + count
  - [ ] C. 목록에 카테고리명 포함: 멀티 join + Projections 중첩 (pms→cms 경계 결합, 학습용 예외)
  - [ ] D. 벌크 update: 좋아요/조회수 증가 (count 도메인 도입 시)
  - [ ] E. 기간별 글 조회: between/goe/loe 범위 조건
  - A(내 글 목록)는 위 "내 글 목록" 항목에서 구현. legacy BoardCustomRepository 기법, cross-domain join은 QueryClient로 회피(C는 예외)
- [ ] (Infra) GitHub Actions 빌드 캐시 적용 (api-v2, web-v2 — Docker 레이어 캐시)
- [ ] (api-v1) ECS 헬스체크 설정 (actuator 의존성 + Task Definition healthCheck)
- [ ] (Infra) 신규 테이블 추가 시 RDS DDL·인덱스 직접 실행 (validate 모드 — 현재 테이블은 적용 완료)
- [ ] (web-v2/Observability) 운영: staging/prod OTEL secret 주입, Grafana Tempo trace 수신 확인
- [ ] (web-v2) 학습·검토: 서버/클라이언트 컴포넌트 이해, cookie 토큰 전환 시 middleware route 보호, route group 구조
- [ ] (api-v1, api-v2) MSA 전환
- [ ] (search) 검색 시스템 도입.
  - [ ] pms vs search 호출 기준: 단건 정확 조회·쓰기·"내 것"(개인화+권한) = pms / 공개 다건 탐색(키워드·카테고리·섹션 목록·필터·정렬·집계) = search
  - [ ] CQRS: pms = 쓰기 원본(MySQL, 권한·개인화) / search = 읽기 최적화(OpenSearch). 동기화는 pms 쓰기 → 변경 이벤트(SQS/Kinesis) → indexer가 MySQL 읽어 문서화 → OpenSearch 인덱싱 (최종 일관성)
  - [ ] QueryDSL vs search 역할: QueryDSL은 관계형 복잡성(권한 한정 "내 것/정확"), search는 탐색 복잡성(풀텍스트·연관도·패싯, 공개 카탈로그 발견)
  - [ ] 도입 전략: 초기엔 pms cursor 목록으로 시작 → 검색·복잡 필터·대량 트래픽 필요 시 OpenSearch+indexer 도입(YAGNI). 별도 서비스 예정(yologram-search-api, yologram-search-indexer)

## 보류/제외 (현재 범위 밖)
- [ ] (UMS) OAuth 로그인 (Gmail, Kakao)
- [ ] (UMS) 프로필 이미지 업로드
- [ ] (web-v2/Observability) browser RUM, client-side tracing, logs, custom metrics
  - 향후 선택지: Grafana Alloy + spanmetrics로 trace에서 request 메트릭 추출, access log 보강


