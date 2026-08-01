from typing import Protocol

from sqlalchemy.orm import Session

from app.domain.tech.comment.repository import TechPostCommentRepository


class TechPostCommentCleanupClient(Protocol):
    """
    tech post → tech comment 도메인 경계 호출 추상화 (게시글 삭제 시 연관 댓글 정리).
    모놀리식에서는 comment 리포지토리를 직접 호출(LocalTechPostCommentCleanupClient),
    MSA 분리 시 comment-api 호출 또는 post-deleted 이벤트 발행 구현으로 교체한다.
    """

    def delete_by_post_id(self, post_id: int) -> None:
        """post_id 게시글의 댓글 전체 삭제 (고아 댓글 방지)."""
        ...


class LocalTechPostCommentCleanupClient:

    def __init__(self, db: Session):
        self.repository = TechPostCommentRepository(db)

    def delete_by_post_id(self, post_id: int) -> None:
        self.repository.delete_by_post_id(post_id)
