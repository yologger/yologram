import logging

from sqlalchemy.orm import Session

from app.domain.pms.tech.repository import TechPostRepository
from app.domain.search.tech.indexing_publisher import AdminTechIndexingPublisher
from app.domain.search.tech.publisher.message.tech_indexing_message import TARGET_TECH_POST
from app.domain.search.tech.publisher.message.tech_indexing_message_publisher import (
    TechIndexingMessagePublisher,
)

logger = logging.getLogger(__name__)


class AdminTechPostIndexingService:
    """
    게시글 인덱싱 요청 (api-v1 AdminTechPostIndexingService 미러) —
    쪼개기·발행은 AdminTechIndexingPublisher가 하고 여기서는 대상과 범위만 정한다.

    pms 리포지토리를 직접 참조한다 — 같은 앱 안의 읽기 전용 조회(max id)이고,
    인덱싱은 pms 데이터를 검색용으로 복제하는 작업이라 경계를 넘는 것이 본질이다.
    """

    def __init__(self, db: Session, publisher: TechIndexingMessagePublisher | None = None):
        self.post_repository = TechPostRepository(db)
        self.indexing_publisher = AdminTechIndexingPublisher(publisher)

    def index(self, id: int) -> None:
        self.indexing_publisher.publish_single(TARGET_TECH_POST, id)

    def index_range(self, from_id: int, to_id: int) -> int:
        return self.indexing_publisher.publish_range(TARGET_TECH_POST, from_id, to_id)

    def full_index(self) -> int:
        return self.indexing_publisher.publish_full(TARGET_TECH_POST, self.post_repository.find_max_id())

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
