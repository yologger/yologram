# yologram-api-v2

FastAPI 기반 API 서버.

## 사전 준비

- Python 3.12+
- uv (https://docs.astral.sh/uv/)

## 의존성 설치

```bash
uv sync
```

## 로컬 실행

기본 프로파일 (default):
```bash
uv run uvicorn app.main:app --reload
```

프로파일 지정:
```bash
APP_PROFILE=prod uv run uvicorn app.main:app --reload
APP_PROFILE=staging uv run uvicorn app.main:app --reload
```

서버 기본 주소: http://localhost:8000

## API 엔드포인트

| 메서드 | 경로 | 설명 |
|---|---|---|
| GET | /api/v2/test | 기본 응답 |
| GET | /api/v2/test/echo | 클라이언트 요청 정보 반환 |
| GET | /api/v2/test/profile | 활성 프로파일 반환 |
| GET | /api/v2/test/property?key=... | 설정값 조회 |

API 문서: http://localhost:8000/docs
