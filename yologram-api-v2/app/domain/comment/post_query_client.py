from typing import Protocol

from sqlalchemy.orm import Session

from app.domain.pms.repository import PostRepository


class PostQueryClient(Protocol):
    """
    comment → pms 도메인 경계 호출 추상화 (대상 글 존재 검증).
    모놀리식에서는 pms 리포지토리를 직접 호출(LocalPostQueryClient),
    MSA 분리 시 pms-api HTTP 호출 구현으로 교체한다.
    """

    def exists(self, post_id: int) -> bool:
        """post_id의 게시글이 존재하면 True."""
        ...


class LocalPostQueryClient:

    def __init__(self, db: Session):
        self.repository = PostRepository(db)

    def exists(self, post_id: int) -> bool:
        return self.repository.find_by_id(post_id) is not None
