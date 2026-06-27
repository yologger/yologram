# 기능 구현 시 따라야 할 내용

### API 경로 규칙
- /api/{v1|v2}/ums/ (유저), /pms/{section}/ (게시글), /cms/{section}/categories (카테고리)
- /comments/ (댓글, 추후), /count/ (카운트, 예약), /news/ (뉴스, 추후)
- 어드민: 도메인 경로 뒤 admin 세그먼트 → /{domain}/admin/... (게이트웨이 라우팅과 일관)

### QueryDSL 사용 기준 (api-v1)
- 단순 단건·고정 조건(findBy/existsBy)·기본 CRUD → Spring Data JpaRepository로 충분
- 다음 중 하나라도 해당하면 QueryDSL custom repository: ①동적 조건 ②다중 조인(2개+ 엔티티) ③projection(필요 컬럼만 DTO) ④조건부 정렬/cursor·offset 페이지네이션 ⑤벌크 update·delete
- yologram 예: "내 글 목록"(section·작성자 필터 + 카테고리 조인 + 페이지네이션) = pms + QueryDSL / 공개 섹션 피드·키워드 검색 = search
- 구현 메모: PostRepositoryImpl.findPostsBySection이 첫 QueryDSL 사용처 — 동적 조건(categoryId/cursor) + EXISTS 서브쿼리 + keyset
- 함정: enum을 담는 패키지명에 `enum`(Java 예약어) 금지. QueryDSL APT가 `import ...enum.Xxx`를 생성 못 해 해당 enum 필드가 Q클래스에서 통째 누락됨(다른 타입 필드는 정상). cms.enum → cms.enums로 해결. ums.enum도 동일 잠재 이슈(현재 QueryDSL 미사용이라 보류)

### MSA 
-  지금은 모놀리틱이나 추후 MSA로 전환 예정이라, 가능하면 아래 내용을 참고하여 MSA 전환이 쉬운 구조로 구현
-  현재 경계 호출은 인터페이스(QueryClient/Protocol)로 추상화 → 분리 시 HTTP/gRPC 구현으로 교체 (예: PostCategoryQueryClient, UserQueryClient). 모놀리식은 Local*QueryClient(리포지토리 직접) 구현, self HTTP 호출 지양
-  단일 서버에서 도메인별 패키지로 분리. 도메인 경계 = 미래 MSA 경계 = API Gateway path prefix
  -  UMS→yologram-user-api, PMS→yologram-post-api, CMS→yologram-cms-api, Comment→comment-api, Count→count-api, News→news-api
-  분리 전략: 도메인 간 직접 JOIN·repository 교차 호출 금지
-  경계 호출은 인터페이스(QueryClient/Protocol)로 추상화 → 분리 시 HTTP/gRPC 구현으로 교체 (예: PostCategoryQueryClient, UserQueryClient). 모놀리식은 Local*QueryClient(리포지토리 직접) 구현, self HTTP 호출 지양
-  경계 넘는 FK 지양(같은 도메인 내부만 FK). 경계 넘는 참조는 인덱스 + app-level 검증
-  경계 넘는 동기 트랜잭션 의존 최소화 (count 갱신은 추후 이벤트/최종일관성)
