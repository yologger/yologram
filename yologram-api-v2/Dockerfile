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
# 조회 이벤트 발행 대상 스트림 (aws/global/kinesis, PROVISIONED 1샤드 · ap-northeast-2).
# 이 이미지는 prod ECS 전용이라 여기가 api-v1 application-prod.yaml에 대응하는 자리 —
# 로컬 실행(uvicorn·scripts/run-prod.sh)은 이 변수가 없어 빈 값 = 발행 스킵
ENV POST_VIEW_STREAM_NAME=yologram-post-view-event-prod
ENTRYPOINT ["uvicorn", "app.main:app", "--host", "0.0.0.0", "--port", "5000"]
