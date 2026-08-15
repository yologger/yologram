import json
import logging
from typing import Protocol

from app.config.settings import get_settings
from app.config.sqs import get_sqs_client
from app.domain.search.tech.publisher.message.tech_indexing_message import TechIndexingMessage

logger = logging.getLogger(__name__)


class TechIndexingMessagePublisher(Protocol):
    """인덱싱 작업 발행 추상화 (PostViewEventPublisher와 동일한 Protocol 패턴).
    도메인 서비스는 이 인터페이스만 알고 AWS SDK(boto3)는 구현에 격리한다."""

    def publish(self, message: TechIndexingMessage) -> None: ...


class SqsTechIndexingMessagePublisher:
    """
    인덱싱 작업 SQS 발행 (api-v1 TechIndexingMessagePublisher 미러).

    조회 이벤트 발행과 실패 처리 방침이 반대다: 조회 이벤트는 사용자 응답을 막지 않아야 해서
    예외를 삼키지만, 인덱싱은 어드민이 명시적으로 요청한 작업이라 실패를 알려야 한다 — 예외를 전파한다.

    큐 URL은 이름으로 조회한 뒤 캐시한다 — 매 발행마다 GetQueueUrl을 부르지 않는다.
    """

    def __init__(self, queue_name: str | None = None, enabled: bool | None = None):
        # 설정만 미리 읽는다 — 클라이언트는 발행 시점 lazy 생성(자격증명 없는 테스트·CI 부팅 보호)
        settings = get_settings()
        self.enabled = settings.post_index_publish_enabled if enabled is None else enabled
        self.queue_name = settings.post_index_publish_queue if queue_name is None else queue_name
        self._queue_url: str | None = None

    def is_enabled(self) -> bool:
        """발행 가능 여부 — 호출부가 미리 확인해 불필요한 범위 계산을 건너뛸 수 있다"""
        return self.enabled and bool(self.queue_name and self.queue_name.strip())

    def publish(self, message: TechIndexingMessage) -> None:
        # 발행 비활성(로컬·테스트 기본)이면 스킵 — prod 큐 오염 방지
        if not self.enabled:
            logger.info(
                f"post index publish disabled — skipped: from={message.from_id} to={message.to_id}"
            )
            return

        if not self.queue_name or not self.queue_name.strip():
            # 켰는데 대상이 없는 설정 실수 — 조용히 스킵하면 인덱싱이 0건인 이유를 알 수 없다
            logger.warning("post index publish is enabled but queue is not configured — skipped")
            return

        client = get_sqs_client()
        if self._queue_url is None:
            self._queue_url = client.get_queue_url(QueueName=self.queue_name)["QueueUrl"]

        client.send_message(
            QueueUrl=self._queue_url,
            # ensure_ascii=False + 구분자 공백 제거 — api-v1(Jackson) 직렬화와 바이트 수준 동일
            MessageBody=json.dumps(message.to_payload(), ensure_ascii=False, separators=(",", ":")),
        )
        logger.info(
            f"post index message sent: target={message.target} range={message.from_id}-{message.to_id}"
        )
