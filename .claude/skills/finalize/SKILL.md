---
name: finalize
description: 기능 구현 후 문서 최신화, Swagger 검토, commit까지 수행하는 마무리 스킬
disable-model-invocation: false
---

## 작업 지시

아래 3단계를 순서대로 수행.

### 1단계: 문서 최신화

git diff로 변경사항을 확인하고, 변경된 프로젝트의 문서를 검토하여 업데이트가 필요한 부분을 최신화:

1. 루트 docs/ 확인 (todos.md, done.md, rules.md로 통합 관리, 프로젝트 구분 없는 평면 구조)
   - docs/todos.md: 구현해야 할 기능. 우선순위 순 기능 목록. 완료된 항목(미러링 기능은 해당 프로젝트 하위 체크박스)을 체크/제거하고 새 할 일 추가. 도메인 태그는 선택
   - docs/done.md: 구현 완료된 기능. 이번에 구현 완료한 기능을 「구현된 기능」(대략 구현 순서)에, 설계 근거는 「설계 근거」(주제별)에 기록. 미러링 기능은 한 항목에 백엔드/프론트 함께 표기
   - docs/rules.md: 구현 시 따라야 할 제약·참고사항. 이번 작업으로 규약(경로 규칙·호출 기준 등)이 바뀌었으면 반영
2. 변경된 프로젝트의 CLAUDE.md, AGENTS.md, README.md 확인
   - 기술 스택, 설정, 포트, 경로 등 변경된 내용 반영
   - CLAUDE.md와 AGENTS.md는 항상 동기화: CLAUDE.md를 수정하면 AGENTS.md에도 동일한 내용을 반영 (반대도 동일)
3. 루트 CLAUDE.md, AGENTS.md 확인
   - 프로젝트 전체에 영향을 주는 변경사항 반영
   - 루트도 마찬가지로 CLAUDE.md 수정 시 AGENTS.md를 동일하게 반영
4. 메모리 파일 확인
   - 변경사항으로 인해 메모리가 outdated 되었으면 업데이트

### 2단계: Swagger 검토

변경사항에 새 API 엔드포인트가 포함된 경우:

1. @Tag, @Operation(summary), @ApiResponses 어노테이션 존재 여부 확인
2. 누락된 어노테이션이 있으면 추가
3. 기존 API의 Swagger 어노테이션이 변경사항과 불일치하면 수정

### 3단계: Commit

1. 문서/Swagger 변경사항이 있으면 기능 코드와 함께 commit
2. 커밋 메시지는 프로젝트 커밋 컨벤션을 따름
   - 형식: [프로젝트명] 타입: 설명
3. commit 전 변경 내용을 사용자에게 보여주고 승인 후 진행

## 규칙

- 변경사항과 관련 없는 문서는 수정하지 않음
- CLAUDE.md와 AGENTS.md는 같은 내용을 담는 쌍이므로 한쪽을 수정하면 다른 쪽도 동일하게 수정 (api-v1/api-v2/web-v1/web-v2/루트 모두 적용)
- 수정 전 계획을 먼저 보여주고 승인 후 적용
- 실제로 변경된 내용만 문서에 반영 (추측으로 추가하지 않음)
- commit 메시지에 Co-Authored-By 추가하지 않음
- push는 하지 않음
