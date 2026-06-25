---
name: finalize
description: 기능 구현 후 문서 최신화, Swagger 검토, commit까지 수행하는 마무리 스킬
disable-model-invocation: false
---

## 작업 지시

아래 3단계를 순서대로 수행.

### 1단계: 문서 최신화

git diff로 변경사항을 확인하고, 변경된 프로젝트의 문서를 검토하여 업데이트가 필요한 부분을 최신화:

1. 변경된 프로젝트의 docs/ 하위 파일 확인 (tasks.md, features.md 2개로 관리)
   - tasks.md: 완료된 항목은 제거하고 새 할 일 추가 (앞으로 할 일만 유지)
   - features.md: 이번에 구현 완료한 기능과 그 설계 근거를 기록 (tasks에서 완료된 내용을 이쪽으로 이전)
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
