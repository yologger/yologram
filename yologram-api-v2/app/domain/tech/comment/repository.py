from sqlalchemy import func
from sqlalchemy.orm import Session

from app.domain.tech.comment.model import TechPostComment
from app.domain.tech.comment.sort import CommentSort


class TechPostCommentRepository:

    def __init__(self, db: Session):
        self.db = db

    def save(self, comment: TechPostComment) -> TechPostComment:
        self.db.add(comment)
        self.db.flush()
        self.db.refresh(comment)
        return comment

    def find_by_id(self, id: int) -> TechPostComment | None:
        return self.db.query(TechPostComment).filter(TechPostComment.id == id).first()

    def delete(self, comment: TechPostComment) -> None:
        self.db.delete(comment)

    def delete_by_post_id(self, post_id: int) -> None:
        """게시글 삭제 시 해당 글의 댓글 전체 제거 (고아 댓글 방지).
        댓글 N건을 개별 delete 대신 벌크 delete 쿼리 한 번으로 정리."""
        self.db.query(TechPostComment).filter(TechPostComment.post_id == post_id).delete()

    def find_by_post_cursor(
        self, post_id: int, sort: CommentSort, cursor_id: int | None, limit: int
    ) -> list[TechPostComment]:
        """특정 글의 댓글 목록 (cursor 페이지네이션) — 실사용.
        LATEST면 id desc·id<cursor_id, OLDEST면 id asc·id>cursor_id로 이어받는다((post_id, id) keyset)."""
        query = self.db.query(TechPostComment).filter(TechPostComment.post_id == post_id)
        # 커서(선택): 최신순은 과거(id<cursor), 오래된순은 이후(id>cursor)로 이어받는다.
        if cursor_id is not None:
            if sort == CommentSort.LATEST:
                query = query.filter(TechPostComment.id < cursor_id)
            else:
                query = query.filter(TechPostComment.id > cursor_id)
        order = TechPostComment.id.desc() if sort == CommentSort.LATEST else TechPostComment.id.asc()
        return query.order_by(order).limit(limit).all()

    def find_by_post_offset(
        self, post_id: int, sort: CommentSort, offset: int, limit: int
    ) -> list[TechPostComment]:
        """특정 글의 댓글 목록 (offset 페이지네이션) — 학습용.
        cursor 방식(find_by_post_cursor)과 대비되는 offset+count 예시. 조건(post_id)·정렬은 동일."""
        query = self.db.query(TechPostComment).filter(TechPostComment.post_id == post_id)
        order = TechPostComment.id.desc() if sort == CommentSort.LATEST else TechPostComment.id.asc()
        return query.order_by(order).offset(offset).limit(limit).all()

    def count_by_post(self, post_id: int) -> int:
        """특정 글의 댓글 전체 개수 (offset totalCount용). 조건은 find_by_post와 동일."""
        return (
            self.db.query(func.count(TechPostComment.id))
            .filter(TechPostComment.post_id == post_id)
            .scalar()
            or 0
        )
