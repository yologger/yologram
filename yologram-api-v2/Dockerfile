FROM python:3.12-slim AS build
WORKDIR /app
COPY pyproject.toml uv.lock ./
RUN pip install uv && uv sync --frozen --no-dev

FROM python:3.12-slim
WORKDIR /app
COPY --from=build /app/.venv /app/.venv
COPY . .
ENV PATH="/app/.venv/bin:$PATH"
ENV APP_PROFILE=prod
# 기본 타임존 고정 — 컨테이너 기본(UTC)에 좌우되지 않게 (api-v1·worker와 동일, 번장 전 서비스 방식).
# Python은 JVM처럼 코드에서 고정하지 않고 TZ 환경변수로 잡는다
ENV TZ=Asia/Seoul
# 조회 이벤트 발행 대상 스트림 (aws/global/kinesis, PROVISIONED 1샤드 · ap-northeast-2).
# 이 이미지는 prod ECS 전용이라 여기가 api-v1 application-prod.yaml에 대응하는 자리 —
# 로컬 실행(uvicorn·scripts/run-prod.sh)은 이 변수가 없어 빈 값 = 발행 스킵
ENV POST_VIEW_PUBLISH_ENABLED=true
ENV POST_VIEW_PUBLISH_STREAM=yologram-post-view-event-prod
# 검색 인덱싱 작업 발행 (SQS) — api-v1 yologram.messages.publish.post-index.{enabled,queue}에 대응.
# IAM은 태스크 롤의 sqs-send 정책(SendMessage·GetQueueUrl만)이 담당한다
ENV POST_INDEX_PUBLISH_ENABLED=true
ENV POST_INDEX_PUBLISH_QUEUE=yologram-search-indexing-prod
# 검색 스위치 — 접속 3개(URI·USERNAME·PASSWORD)는 task definition secrets로 SSM에서 주입한다.
# api-v1은 yaml(opensearch.main.enabled)에 대응
ENV OPENSEARCH_MAIN_ENABLED=true
ENTRYPOINT ["uvicorn", "app.main:app", "--host", "0.0.0.0", "--port", "5000"]
