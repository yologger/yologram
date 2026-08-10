from sqlalchemy import exists, func
from sqlalchemy.dialects.mysql import insert as mysql_insert
from sqlalchemy.orm import Query, Session

from app.domain.pms.tech.model import (
    TechPost,
    TechPostCategoryMapping,
    TechPostCommentCount,
    TechPostWithCommentCount,
)


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

    # --- 댓글 수 프로젝션 ---

    def _with_comment_count_query(self) -> Query:
        """게시글 + 댓글 수 프로젝션 쿼리. 댓글 수는 tech_post_comment_count를 outerjoin해
        coalesce(0)로 — count row가 없는 글(댓글 0개)도 목록·상세에서 빠지지 않고 0으로 나온다.
        무FK라 조인 조건(post.id == count.post_id)을 명시. 글:카운트가 1:1(카운트 PK=post_id)이라
        join으로 row가 불어나지 않아 기존 정렬·커서·limit에 영향 없다 (api-v1 TechPostRepositoryImpl 미러)."""
        return self.db.query(
            TechPost, func.coalesce(TechPostCommentCount.comment_count, 0)
        ).outerjoin(TechPostCommentCount, TechPost.id == TechPostCommentCount.post_id)

    @staticmethod
    def _to_with_comment_count(rows) -> list[TechPostWithCommentCount]:
        """(TechPost, coalesce 댓글 수) row → 프로젝션 변환. 목록 조회들이 공유."""
        return [TechPostWithCommentCount(post=post, comment_count=count) for post, count in rows]

    def find_post_with_comment_count(self, id: int) -> TechPostWithCommentCount | None:
        """상세 단건 + 댓글 수 (없는 글이면 None → 호출부 404)."""
        row = self._with_comment_count_query().filter(TechPost.id == id).first()
        if row is None:
            return None
        return TechPostWithCommentCount(post=row[0], comment_count=row[1])

    # --- 피드 ---

    def find_posts(
        self, category_id: int | None, cursor_id: int | None, limit: int
    ) -> list[TechPostWithCommentCount]:
        """테크 피드 (id desc), cursor(keyset) 페이지네이션 — 실사용. cursor_id보다 과거 글부터 limit개."""
        query = self._feed_query(self._with_comment_count_query(), category_id)
        # 커서(선택): id가 곧 작성순이므로 id < cursor_id면 더 과거 글
        if cursor_id is not None:
            query = query.filter(TechPost.id < cursor_id)
        return self._to_with_comment_count(query.order_by(TechPost.id.desc()).limit(limit).all())

    def find_posts_offset(
        self, category_id: int | None, offset: int, limit: int
    ) -> list[TechPostWithCommentCount]:
        """테크 피드 (id desc), offset 페이지네이션 — 학습용. cursor와 동일 조건 + offset/limit."""
        query = self._feed_query(self._with_comment_count_query(), category_id)
        return self._to_with_comment_count(
            query.order_by(TechPost.id.desc()).offset(offset).limit(limit).all()
        )

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

    def find_my_posts_by_cursor(
        self, user_id: int, cursor_id: int | None, limit: int
    ) -> list[TechPostWithCommentCount]:
        """내 글 (id desc), cursor 페이지네이션 — 실사용(무한스크롤)."""
        query = self._my_posts_query(self._with_comment_count_query(), user_id)
        if cursor_id is not None:
            query = query.filter(TechPost.id < cursor_id)
        return self._to_with_comment_count(query.order_by(TechPost.id.desc()).limit(limit).all())

    def find_my_posts_by_offset(
        self, user_id: int, offset: int, limit: int
    ) -> list[TechPostWithCommentCount]:
        """내 글 (id desc), offset 페이지네이션 — 학습용."""
        query = self._my_posts_query(self._with_comment_count_query(), user_id)
        return self._to_with_comment_count(
            query.order_by(TechPost.id.desc()).offset(offset).limit(limit).all()
        )

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


class TechPostCommentCountRepository:
    """tech_post_comment_count 원자 갱신 전용 (api-v1 TechPostCommentCountRepository 미러).
    엔티티를 읽어 +1 후 저장하는 방식은 동시 요청 레이스가 있어 금지 — DB에서 한 문장으로 갱신한다."""

    def __init__(self, db: Session):
        self.db = db

    def increase(self, post_id: int) -> None:
        """댓글 수 +1 (댓글 작성 시). row가 없으면 1로 생성 — MySQL upsert(ON DUPLICATE KEY UPDATE)로
        "읽고 +1 후 저장"의 동시 요청 레이스 없이 DB에서 원자 갱신."""
        stmt = mysql_insert(TechPostCommentCount).values(post_id=post_id, comment_count=1)
        stmt = stmt.on_duplicate_key_update(comment_count=TechPostCommentCount.comment_count + 1)
        self.db.execute(stmt)

    def decrease(self, post_id: int) -> None:
        """댓글 수 -1 (댓글 삭제 시). comment_count > 0 조건으로 음수 방어 —
        0이거나 row가 없으면 0건 갱신으로 무해(0에서 더 내려가지 않고, row도 만들지 않는다)."""
        (
            self.db.query(TechPostCommentCount)
            .filter(TechPostCommentCount.post_id == post_id, TechPostCommentCount.comment_count > 0)
            .update(
                {TechPostCommentCount.comment_count: TechPostCommentCount.comment_count - 1},
                synchronize_session=False,
            )
        )
