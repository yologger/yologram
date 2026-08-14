import logging

from sqlalchemy.orm import Session

from app.core.exception import InvalidIndexRangeException
from app.domain.pms.tech.repository import TechPostRepository
from app.domain.search.tech.publisher.message.tech_post_index_message import TechPostIndexMessage
from app.domain.search.tech.publisher.message.tech_post_index_message_publisher import (
    SqsTechPostIndexMessagePublisher,
    TechPostIndexMessagePublisher,
)

logger = logging.getLogger(__name__)

# 메시지 1건이 담는 id 범위 크기 (api-v1 CHUNK_SIZE·레거시 BULK_INDEXING_REQUEST_BATCH_SIZE 미러)
CHUNK_SIZE = 20


class AdminTechPostIndexingService:
    """
    게시글 인덱싱 요청 (api-v1 AdminTechPostIndexingService 미러) —
    실제 인덱싱은 하지 않고 SQS에 작업만 넣는다. 소비·색인은 worker가 담당하므로
    이 서비스는 OpenSearch 클라이언트를 갖지 않는다.

    큰 범위를 한 메시지에 담지 않고 CHUNK_SIZE로 쪼개는 이유:
      ① 한 메시지의 처리 시간이 SQS 가시성 타임아웃(300초)을 넘으면 재노출돼 중복 처리된다
      ② 실패 시 재시도 단위가 작아진다(10만 건 한 덩어리가 아니라 20건)
      ③ 워커를 늘리면 메시지 단위로 병렬 처리된다

    pms 리포지토리를 직접 참조한다 — 같은 앱 안의 읽기 전용 조회(max id)이고,
    인덱싱은 pms 데이터를 검색용으로 복제하는 작업이라 경계를 넘는 것이 본질이다.
    """

    def __init__(self, db: Session, publisher: TechPostIndexMessagePublisher | None = None):
        self.post_repository = TechPostRepository(db)
        self.publisher = publisher or SqsTechPostIndexMessagePublisher()

    def index(self, id: int) -> None:
        """단건 — from == to로 보내 범위 인덱싱과 같은 경로를 탄다"""
        self.publisher.publish(TechPostIndexMessage(from_id=id, to_id=id))

    def index_range(self, from_id: int, to_id: int) -> int:
        """범위 — CHUNK_SIZE 단위로 쪼개 발행. 반환값은 발행한 메시지 수"""
        # 도메인 예외로 던져야 400이 된다 (ValueError는 전역 폴백에서 500이 된다)
        if from_id > to_id or from_id < 1:
            raise InvalidIndexRangeException()

        current = from_id
        published = 0
        while current <= to_id:
            chunk_to = min(current + CHUNK_SIZE - 1, to_id)
            self.publisher.publish(TechPostIndexMessage(from_id=current, to_id=chunk_to))
            published += 1
            current = chunk_to + 1

        logger.info(f"post index requested: range={from_id}-{to_id} messages={published}")
        return published

    def full_index(self) -> int:
        """
        전체 — max id까지 훑는다. 삭제된 id 구간은 워커가 조회 결과 0건으로 흘려보낸다(무해).
        반환값은 발행한 메시지 수, 글이 없으면 0.
        """
        max_id = self.post_repository.find_max_id()
        if max_id is None or max_id <= 0:
            logger.info("no posts to index")
            return 0

        logger.info(f"full post index requested: maxId={max_id}")
        return self.index_range(from_id=1, to_id=max_id)

    def full_index_in_background(self) -> None:
        """
        전체(백그라운드 진입점) — 라우터가 BackgroundTasks로 넘긴다.
        발행 루프를 요청 처리 중에 돌리면 게시글이 늘수록 응답이 늦어져 게이트웨이 타임아웃(30초)에 걸린다
        (api-v1은 @Async("sqsTaskExecutor")로 같은 처리).

        백그라운드 작업의 예외는 호출자에게 전달되지 않으므로 여기서 직접 잡아 남긴다 —
        진행 상황은 예외가 아니라 SQS 큐 깊이로 확인한다.
        """
        try:
            self.full_index()
        except Exception:
            logger.error("full post index publish failed", exc_info=True)
