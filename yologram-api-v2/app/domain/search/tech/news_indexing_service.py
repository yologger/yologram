import logging

from sqlalchemy.orm import Session

from app.domain.news.tech.repository import TechNewsRepository
from app.domain.search.tech.indexing_publisher import AdminTechIndexingPublisher
from app.domain.search.tech.publisher.message.tech_indexing_message import TARGET_TECH_NEWS
from app.domain.search.tech.publisher.message.tech_indexing_message_publisher import (
    TechIndexingMessagePublisher,
)

logger = logging.getLogger(__name__)


class AdminTechNewsIndexingService:
    """
    뉴스 인덱싱 요청 (api-v1 AdminTechNewsIndexingService 미러) —
    게시글과 같은 구조이고 대상(target)만 다르다.

    평상시 뉴스 색인은 worker가 요약 직후 직접 한다(실시간). 이 API는 그 경로가 놓친 구간을 메우는 용도다 —
    색인 실패로 빠진 건, 매핑 변경 후 재색인, 검색을 나중에 켠 경우의 과거 데이터.
    """

    def __init__(self, db: Session, publisher: TechIndexingMessagePublisher | None = None):
        self.news_repository = TechNewsRepository(db)
        self.indexing_publisher = AdminTechIndexingPublisher(publisher)

    def index(self, id: int) -> None:
        self.indexing_publisher.publish_single(TARGET_TECH_NEWS, id)

    def index_range(self, from_id: int, to_id: int) -> int:
        return self.indexing_publisher.publish_range(TARGET_TECH_NEWS, from_id, to_id)

    def full_index(self) -> int:
        return self.indexing_publisher.publish_full(TARGET_TECH_NEWS, self.news_repository.find_max_id())

    def full_index_in_background(self) -> None:
        """전체(백그라운드 진입점) — 게시글과 같은 이유로 요청 처리 중에 발행 루프를 돌리지 않는다"""
        try:
            self.full_index()
        except Exception:
            logger.error("full news index publish failed", exc_info=True)
