from typing import Protocol

from sqlalchemy.orm import Session

from app.domain.tech.post.repository import TechPostRepository


class TechPostQueryClient(Protocol):
    """
    tech comment → tech post 도메인 경계 호출 추상화 (대상 글 존재 검증).
    모놀리식에서는 tech post 리포지토리를 직접 호출(LocalTechPostQueryClient),
    MSA 분리 시 post-api HTTP 호출 구현으로 교체한다.
    """

    def exists(self, post_id: int) -> bool:
        """post_id의 게시글이 존재하면 True."""
        ...


class LocalTechPostQueryClient:

    def __init__(self, db: Session):
        self.repository = TechPostRepository(db)

    def exists(self, post_id: int) -> bool:
        return self.repository.find_by_id(post_id) is not None
