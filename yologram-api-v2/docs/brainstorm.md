# yologram-api-v2 브레인스토밍

## 목표

- yologram-api-v1 (Spring Boot)과 동일한 기능을 FastAPI로 구현
- 두 프로젝트 비교분석을 통한 학습
- 객체지향 설계, Layered Architecture, 의존성 주입 적용

## Spring Boot vs FastAPI 대응 구조

| Spring Boot (v1) | FastAPI (v2) | 비고 |
|---|---|---|
| @RestController | APIRouter (클래스 기반) | FastAPI는 함수 기반이 기본이지만 class-based로 구성 |
| @Service | Service 클래스 | 비즈니스 로직 계층 |
| @Repository | Repository 클래스 | 데이터 접근 계층 (추후) |
| @Autowired / 생성자 주입 | Depends() | FastAPI 의존성 주입 |
| application-{profile}.yaml | .env.{profile} | pydantic-settings 활용 |
| Environment.getProperty() | Settings 클래스 | pydantic BaseSettings |
| Spring Profiles | APP_PROFILE 환경변수 | prod, staging, default 분기 |
| aws-parameterstore import | ECS Task Definition secrets | 인프라 레벨에서 환경변수 주입 |

## 프로젝트 구조안

```
yologram-api-v2/
├── app/
│   ├── main.py                  # FastAPI 앱 엔트리포인트
│   ├── config/
│   │   └── settings.py          # pydantic BaseSettings 기반 설정
│   ├── domain/
│   │   └── test/
│   │       ├── router.py        # Controller 역할 (APIRouter)
│   │       ├── service.py       # 비즈니스 로직
│   │       └── schema.py        # 요청/응답 스키마 (Pydantic)
│   └── core/
│       └── di.py                # 의존성 주입 설정
├── tests/
├── Dockerfile
├── .env                         # 기본 설정
├── .env.prod                    # prod 설정
├── .env.staging                 # staging 설정
├── docs/
│   ├── brainstorm.md
│   ├── plan.md
│   └── tasks.md
└── pyproject.toml               # uv 패키지 관리
```

## 의존성 주입 방식

Spring Boot:
```kotlin
@RestController
class TestResource(
    private val environment: Environment
)
```

FastAPI 대응 (Depends + 팩토리 함수):
```python
def get_test_service() -> TestService:
    return TestService(settings=get_settings())

@router.get("/test")
def index(service: TestService = Depends(get_test_service)):
    ...
```

## 설정 관리 방식

### 로컬/일반 설정: .env 파일

- pydantic-settings의 BaseSettings가 .env를 기본 지원
- APP_PROFILE 환경변수로 .env.prod / .env.staging 분기
- 12-Factor App 원칙에 부합 (환경변수로 설정 관리)
- FastAPI/Python 생태계에서 보편적인 방식

```python
class Settings(BaseSettings):
    app_name: str = "yologram-api-v2"
    app_profile: str = "default"
    model_config = SettingsConfigDict(env_file=".env")
```

### Secret 관리: ECS Task Definition에서 주입

- AWS Parameter Store에 secret 저장
- ECS Task Definition의 secrets 블록으로 컨테이너 환경변수에 주입
- 코드에서는 os.environ으로 접근 (프레임워크 의존성 없음)
- Spring Boot의 aws-parameterstore import와 달리, 인프라 레벨에서 처리

```hcl
container_definitions = jsonencode([{
  secrets = [
    {
      name      = "DB_PASSWORD"
      valueFrom = "arn:aws:ssm:ap-northeast-2:123456:parameter/yologram/db_password"
    }
  ]
}])
```

Spring Boot는 프레임워크가 Parameter Store를 직접 통합하지만,
FastAPI(및 대부분의 프레임워크)는 인프라(ECS, K8s)에 secret 주입을 맡기는 게 표준.

## 구현할 엔드포인트 (v1과 동일)

| 메서드 | 경로 | 설명 |
|---|---|---|
| GET | /api/v2/test | 기본 응답 |
| GET | /api/v2/test/echo | 클라이언트 요청 정보 반환 |
| GET | /api/v2/test/profile | 활성 프로파일 반환 |
| GET | /api/v2/test/property?key=... | 설정값 조회 |

## 결정 사항

- 패키지 매니저: uv
- 설정 관리: pydantic-settings + .env 파일 (YAML 아님)
- Secret 관리: ECS Task Definition secrets (앱 코드에서 직접 안 가져옴)
- Python 버전: 3.12+

## 기술 스택 정리

- Python 3.12+
- FastAPI
- Uvicorn (ASGI 서버)
- Pydantic + pydantic-settings (설정 관리)
- uv (패키지 매니저)

## 회원탈퇴 데이터 정리 전략 (구현 시 결정)

탈퇴 요청 처리와 연관 데이터(게시글/댓글/좋아요 등) 삭제를 분리해 요청 부하를 낮춘다.

- 1차(동기, 즉시 응답): User.status=DELETED, deleted_date 기록, access_token 무효화 후 204 반환. 조회 단계에서 DELETED 유저 데이터는 필터링 → 사용자 입장에선 즉시 탈퇴.
- 2차(비동기, 연관 데이터 실제 삭제) 옵션:
  - SQS 이벤트 + 워커 (권장, 확장성): "user-deleted" 발행 → 워커가 도메인별 배치 삭제, 실패 시 DLQ 재시도
  - 배치 잡 (간단): DELETED 유저를 주기적으로 스캔해 청크 삭제, 별도 인프라 최소
  - 앱 내 BackgroundTasks (소규모/임시): 요청과 분리하나 인스턴스 종속 → 배포/장애 시 유실 위험, 대량 부적합
- 대량 삭제는 청크(LIMIT N) 단위 반복으로 락/replica 지연 완화
- 보관 의무 데이터는 삭제 대신 익명화, 유예기간(복구) 정책 검토 (soft delete면 자연스럽게 지원)
- 권장 조합: soft delete 즉시 응답 + SQS 워커의 도메인별·청크 삭제 (규모 작으면 배치 잡으로 시작)
