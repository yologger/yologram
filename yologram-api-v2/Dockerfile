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
ENTRYPOINT ["uvicorn", "app.main:app", "--host", "0.0.0.0", "--port", "5000"]
