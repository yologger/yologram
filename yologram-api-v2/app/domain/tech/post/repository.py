from sqlalchemy import exists, func
from sqlalchemy.orm import Query, Session

from app.domain.tech.post.model import TechPost, TechPostCategoryMapping


class TechPostRepository:

    def __init__(self, db: Session):
        self.db = db

    def save(self, post: TechPost) -> TechPost:
        self.db.add(post)
        self.db.flush()
        self.db.refresh(post)
        return post

    def find_by_id(self, id: int) -> TechPost | None:
        return self.db.query(TechPost).filter(TechPost.id == id).first()

    def delete(self, post: TechPost) -> None:
        self.db.delete(post)

    # --- 피드 ---

    def find_posts(self, category_id: int | None, cursor_id: int | None, limit: int) -> list[TechPost]:
        """테크 피드 (id desc), cursor(keyset) 페이지네이션 — 실사용. cursor_id보다 과거 글부터 limit개."""
        query = self._feed_query(self.db.query(TechPost), category_id)
        # 커서(선택): id가 곧 작성순이므로 id < cursor_id면 더 과거 글
        if cursor_id is not None:
            query = query.filter(TechPost.id < cursor_id)
        return query.order_by(TechPost.id.desc()).limit(limit).all()

    def find_posts_offset(self, category_id: int | None, offset: int, limit: int) -> list[TechPost]:
        """테크 피드 (id desc), offset 페이지네이션 — 학습용. cursor와 동일 조건 + offset/limit."""
        query = self._feed_query(self.db.query(TechPost), category_id)
        return query.order_by(TechPost.id.desc()).offset(offset).limit(limit).all()

    def count_posts(self, category_id: int | None) -> int:
        """테크 피드 전체 개수 (offset totalCount용). 조건은 find_posts와 동일."""
        query = self._feed_query(self.db.query(func.count(TechPost.id)), category_id)
        return query.scalar() or 0

    def _feed_query(self, query: Query, category_id: int | None) -> Query:
        """피드 동적 조건(카테고리 선택). cursor/offset/count가 공유. 카테고리 필터는 EXISTS(1:N 안전)."""
        if category_id is not None:
            query = query.filter(
                exists().where(
                    (TechPostCategoryMapping.post_id == TechPost.id)
                    & (TechPostCategoryMapping.category_id == category_id)
                )
            )
        return query

    # --- 내 글 ---

    def find_my_posts_by_cursor(self, user_id: int, cursor_id: int | None, limit: int) -> list[TechPost]:
        """내 글 (id desc), cursor 페이지네이션 — 실사용(무한스크롤)."""
        query = self._my_posts_query(self.db.query(TechPost), user_id)
        if cursor_id is not None:
            query = query.filter(TechPost.id < cursor_id)
        return query.order_by(TechPost.id.desc()).limit(limit).all()

    def find_my_posts_by_offset(self, user_id: int, offset: int, limit: int) -> list[TechPost]:
        """내 글 (id desc), offset 페이지네이션 — 학습용."""
        query = self._my_posts_query(self.db.query(TechPost), user_id)
        return query.order_by(TechPost.id.desc()).offset(offset).limit(limit).all()

    def count_my_posts(self, user_id: int) -> int:
        """내 글 전체 개수 (offset totalCount용). 조건은 find_my_posts와 동일."""
        query = self._my_posts_query(self.db.query(func.count(TechPost.id)), user_id)
        return query.scalar() or 0

    def _my_posts_query(self, query: Query, user_id: int) -> Query:
        """내 글 조건(user_id). cursor/offset/count가 공유."""
        return query.filter(TechPost.user_id == user_id)


class TechPostCategoryMappingRepository:

    def __init__(self, db: Session):
        self.db = db

    def save(self, post_category: TechPostCategoryMapping) -> TechPostCategoryMapping:
        self.db.add(post_category)
        self.db.flush()
        return post_category

    def find_by_post_id(self, post_id: int) -> list[TechPostCategoryMapping]:
        return (
            self.db.query(TechPostCategoryMapping)
            .filter(TechPostCategoryMapping.post_id == post_id)
            .all()
        )

    def find_by_post_ids(self, post_ids: list[int]) -> list[TechPostCategoryMapping]:
        if not post_ids:
            return []
        return (
            self.db.query(TechPostCategoryMapping)
            .filter(TechPostCategoryMapping.post_id.in_(post_ids))
            .all()
        )

    def delete_by_post_id(self, post_id: int) -> None:
        """게시글 수정/삭제 시 카테고리 매핑 전체 제거 (수정은 제거 후 재생성으로 교체)."""
        self.db.query(TechPostCategoryMapping).filter(TechPostCategoryMapping.post_id == post_id).delete()
