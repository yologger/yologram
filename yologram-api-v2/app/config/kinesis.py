from functools import lru_cache
from typing import Any

import boto3
from botocore.config import Config


@lru_cache
def get_kinesis_client() -> Any:
    """
    이벤트 발행용 Kinesis 클라이언트 — lru_cache로 앱 수명주기 동안 재사용(요청마다 생성하면 세션·설정 로딩이 반복된다).
    리전은 SesEmailSender와 동일 관례(ap-northeast-2 고정), 자격증명은 기본 체인
    (로컬: AWS_PROFILE 환경변수, prod: ECS Task Role) — 코드에 키를 두지 않는다.

    조회 이벤트 발행은 부가 기능 — Kinesis 지연이 상세 조회 응답으로 전파되지 않도록 타임아웃 1초·재시도 없음
    (api-v1 KinesisConfig과 동일 근거: 기본값은 연결 60초·읽기 60초 + 재시도 다수라 장애 시 요청이 그대로 매달린다).
    total_max_attempts=1은 초기 요청 포함 "전체 시도 1회" = 재시도 없음 (max_attempts는 재시도 횟수라 의미가 다름).
    최악 지연은 connect 1초 + read 1초 (api-v1의 apiCallTimeout 1초와 같은 수준).

    호출부(KinesisPostViewEventPublisher)는 스트림 이름이 설정된 경우에만 이 함수를 부른다 —
    자격증명 없는 환경(테스트·CI)에서 클라이언트가 아예 만들어지지 않아 부팅·임포트에 영향이 없다.
    """
    return boto3.client(
        "kinesis",
        region_name="ap-northeast-2",
        config=Config(
            connect_timeout=1,
            read_timeout=1,
            retries={"total_max_attempts": 1, "mode": "standard"},
        ),
    )
