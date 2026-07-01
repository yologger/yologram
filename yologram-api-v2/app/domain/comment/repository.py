from sqlalchemy import func
from sqlalchemy.orm import Session

from app.domain.comment.model import Comment
from app.domain.comment.sort import CommentSort


class CommentRepository:

    def __init__(self, db: Session):
        self.db = db

    def save(self, comment: Comment) -> Comment:
        self.db.add(comment)
        self.db.flush()
        self.db.refresh(comment)
        return comment

    def find_by_post_cursor(
        self, post_id: int, sort: CommentSort, cursor_id: int | None, limit: int
    ) -> list[Comment]:
        """특정 글의 댓글 목록 (cursor 페이지네이션) — 실사용.
        LATEST면 id desc·id<cursor_id, OLDEST면 id asc·id>cursor_id로 이어받는다((post_id, id) keyset)."""
        query = self.db.query(Comment).filter(Comment.post_id == post_id)
        # 커서(선택): 최신순은 과거(id<cursor), 오래된순은 이후(id>cursor)로 이어받는다.
        if cursor_id is not None:
            if sort == CommentSort.LATEST:
                query = query.filter(Comment.id < cursor_id)
            else:
                query = query.filter(Comment.id > cursor_id)
        order = Comment.id.desc() if sort == CommentSort.LATEST else Comment.id.asc()
        return query.order_by(order).limit(limit).all()

    def find_by_post_offset(
        self, post_id: int, sort: CommentSort, offset: int, limit: int
    ) -> list[Comment]:
        """특정 글의 댓글 목록 (offset 페이지네이션) — 학습용.
        cursor 방식(find_by_post_cursor)과 대비되는 offset+count 예시. 조건(post_id)·정렬은 동일."""
        query = self.db.query(Comment).filter(Comment.post_id == post_id)
        order = Comment.id.desc() if sort == CommentSort.LATEST else Comment.id.asc()
        return query.order_by(order).offset(offset).limit(limit).all()

    def count_by_post(self, post_id: int) -> int:
        """특정 글의 댓글 전체 개수 (offset totalCount용). 조건은 find_by_post와 동일."""
        return self.db.query(func.count(Comment.id)).filter(Comment.post_id == post_id).scalar() or 0
