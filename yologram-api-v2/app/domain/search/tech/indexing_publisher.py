import logging

from app.core.exception import InvalidIndexRangeException
from app.domain.search.tech.publisher.message.tech_indexing_message import TechIndexingMessage
from app.domain.search.tech.publisher.message.tech_indexing_message_publisher import (
    SqsTechIndexingMessagePublisher,
    TechIndexingMessagePublisher,
)

logger = logging.getLogger(__name__)

# 메시지 1건이 담는 id 범위 크기 (api-v1 CHUNK_SIZE·레거시 BULK_INDEXING_REQUEST_BATCH_SIZE 미러)
CHUNK_SIZE = 20


class AdminTechIndexingPublisher:
    """
    인덱싱 작업 발행 공통 로직 (api-v1 AdminTechIndexingPublisher 미러) —
    대상(게시글·뉴스)이 달라도 쪼개는 방식은 같다.

    큰 범위를 한 메시지에 담지 않고 CHUNK_SIZE로 쪼개는 이유:
      ① 한 메시지의 처리 시간이 SQS 가시성 타임아웃(300초)을 넘으면 재노출돼 중복 처리된다
      ② 실패 시 재시도 단위가 작아진다(10만 건 한 덩어리가 아니라 20건)
      ③ 워커를 늘리면 메시지 단위로 병렬 처리된다

    실제 인덱싱은 하지 않는다 — 소비·색인은 worker가 담당하므로 OpenSearch 클라이언트를 갖지 않는다.
    """

    def __init__(self, publisher: TechIndexingMessagePublisher | None = None):
        self.publisher = publisher or SqsTechIndexingMessagePublisher()

    def publish_single(self, target: str, id: int) -> None:
        """단건 — from == to로 보내 범위 인덱싱과 같은 경로를 탄다"""
        self.publisher.publish(TechIndexingMessage(target=target, from_id=id, to_id=id))

    def publish_range(self, target: str, from_id: int, to_id: int) -> int:
        """범위를 CHUNK_SIZE 단위로 쪼개 발행. 반환값은 발행한 메시지 수"""
        # 도메인 예외로 던져야 400이 된다 (ValueError는 전역 폴백에서 500이 된다)
        if from_id > to_id or from_id < 1:
            raise InvalidIndexRangeException()

        current = from_id
        published = 0
        while current <= to_id:
            chunk_to = min(current + CHUNK_SIZE - 1, to_id)
            self.publisher.publish(TechIndexingMessage(target=target, from_id=current, to_id=chunk_to))
            published += 1
            current = chunk_to + 1

        logger.info(f"index requested: target={target} range={from_id}-{to_id} messages={published}")
        return published

    def publish_full(self, target: str, max_id: int | None) -> int:
        """
        전체 — max id까지 훑는다. 삭제된 id 구간은 워커가 조회 결과 0건으로 흘려보낸다(무해).
        반환값은 발행한 메시지 수, 대상이 없으면 0.
        """
        if max_id is None or max_id <= 0:
            logger.info(f"no documents to index: target={target}")
            return 0

        logger.info(f"full index requested: target={target} maxId={max_id}")
        return self.publish_range(target, from_id=1, to_id=max_id)
