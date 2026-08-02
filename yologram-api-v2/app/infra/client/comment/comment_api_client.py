# 타 도메인 경계 클라이언트 — 타 도메인 리포지토리 import는 app/infra/client에서만 허용 (domain 간 직접 참조 금지).
# MSA 분리 시 Local 구현 대신 Rest(HTTP) 구현을 추가해 교체한다.
from typing import Protocol

from sqlalchemy.orm import Session

from app.domain.comment.tech.repository import TechPostCommentRepository


class CommentApiClient(Protocol):
    """
    tech post → tech comment 도메인 경계 호출 추상화 (게시글 삭제 시 연관 댓글 정리).
    모놀리식에서는 comment 리포지토리를 직접 호출(LocalCommentApiClient),
    MSA 분리 시 comment-api 호출 또는 post-deleted 이벤트 발행 구현으로 교체한다.
    """

    def delete_by_post_id(self, post_id: int) -> None:
        """post_id 게시글의 댓글 전체 삭제 (고아 댓글 방지)."""
        ...


class LocalCommentApiClient:

    def __init__(self, db: Session):
        self.repository = TechPostCommentRepository(db)

    def delete_by_post_id(self, post_id: int) -> None:
        self.repository.delete_by_post_id(post_id)
