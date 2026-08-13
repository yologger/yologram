import json
import logging
from typing import Protocol

from app.config.kinesis import get_kinesis_client
from app.config.settings import get_settings
from app.domain.pms.tech.publisher.event.post_view_event import PostViewEvent

logger = logging.getLogger(__name__)


class PostViewEventPublisher(Protocol):
    """조회 이벤트 발행 추상화 (CacheService·EmailSender와 동일한 Protocol 패턴).
    도메인 서비스는 이 인터페이스만 알고 AWS SDK(boto3)는 infra 구현에 격리한다(경계, api-v1 infra/event 미러).
    구현은 실패를 삼켜 상세 조회 응답을 막지 않는다."""

    def publish(self, event: PostViewEvent) -> None: ...


class KinesisPostViewEventPublisher:
    """
    게시글 조회 이벤트 Kinesis 발행 (api-v1 PostViewEventPublisher 미러).

    조회 이벤트는 부가 기능이므로 RedisCacheService와 동일한 원칙: 예외를 삼키고 warning 로그만 남긴다
    (발행 실패가 상세 조회 응답을 막지 않는다 — 조회 API 가용성 우선).
    """

    def __init__(self, stream_name: str | None = None, enabled: bool | None = None):
        # 설정만 미리 읽는다 — 클라이언트는 발행 시점 lazy 생성(자격증명 없는 테스트·CI 부팅 보호)
        settings = get_settings()
        self.enabled = settings.post_view_publish_enabled if enabled is None else enabled
        self.stream_name = settings.post_view_publish_stream if stream_name is None else stream_name

    def publish(self, event: PostViewEvent) -> None:
        # 발행 비활성(로컬·테스트 기본)이면 스킵 — prod 스트림 오염 방지
        if not self.enabled:
            return

        if not self.stream_name or not self.stream_name.strip():
            # 켰는데 대상이 없는 설정 실수 — 조용히 스킵하면 발행이 0건인 이유를 알 수 없다
            logger.warning(
                f"post view event publish is enabled but stream is not configured — skipped: postId={event.post_id}"
            )
            return

        try:
            get_kinesis_client().put_record(
                StreamName=self.stream_name,
                # partitionKey = postId → 같은 글의 이벤트는 같은 샤드에 들어가 순서가 보장된다
                PartitionKey=str(event.post_id),
                # ensure_ascii=False + 구분자 공백 제거 — api-v1(Jackson) 직렬화와 바이트 수준 동일
                Data=json.dumps(event.to_payload(), ensure_ascii=False, separators=(",", ":")).encode("utf-8"),
            )
        except Exception:
            logger.warning(
                f"unexpected error occurred while publishing post view event: postId={event.post_id}",
                exc_info=True,
            )
