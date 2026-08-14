from functools import lru_cache
from typing import Any

import boto3
from botocore.config import Config


@lru_cache
def get_sqs_client() -> Any:
    """
    인덱싱 작업 발행용 SQS 클라이언트 (api-v1 SqsConfig 미러) —
    lru_cache로 앱 수명주기 동안 재사용(요청마다 생성하면 세션·설정 로딩이 반복된다).
    리전은 get_kinesis_client와 동일 관례(ap-northeast-2 고정), 자격증명은 기본 체인
    (로컬: AWS_PROFILE, prod: ECS Task Role).

    타임아웃은 조회 이벤트(1초)보다 넉넉하다 — 인덱싱은 어드민이 결과를 기다리는 작업이라
    한 번의 일시적 지연으로 실패시키는 것보다 잠깐 기다리는 편이 낫다.
    전체 인덱싱은 청크마다 SendMessage를 반복하므로 재시도를 2회까지 허용한다.

    호출부(SqsTechPostIndexMessagePublisher)는 큐 이름이 설정된 경우에만 이 함수를 부른다 —
    자격증명 없는 환경(테스트·CI)에서 클라이언트가 만들어지지 않아 부팅·임포트에 영향이 없다.
    """
    return boto3.client(
        "sqs",
        region_name="ap-northeast-2",
        config=Config(
            connect_timeout=2,
            read_timeout=2,
            retries={"total_max_attempts": 2, "mode": "standard"},
        ),
    )
