from sqlalchemy import exists, func
from sqlalchemy.dialects.mysql import insert as mysql_insert
from sqlalchemy.orm import Query, Session

from app.domain.pms.tech.model import (
    TechPost,
    TechPostCategoryMapping,
    TechPostCommentCount,
    TechPostLike,
    TechPostLikeCount,
    TechPostViewCount,
    TechPostWithCounts,
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

    # --- 카운트(댓글 수·좋아요 수·조회 수) 프로젝션 ---

    def _with_counts_query(self) -> Query:
        """게시글 + 카운트 프로젝션 쿼리. 각 카운트는 1:1 카운트 테이블을 outerjoin해
        coalesce(0)로 — count row가 없는 글(카운트 0)도 목록·상세에서 빠지지 않고 0으로 나온다.
        무FK라 조인 조건(post.id == count.post_id)을 명시. 글:카운트가 1:1(카운트 PK=post_id)이라
        join으로 row가 불어나지 않아 기존 정렬·커서·limit에 영향 없다 (api-v1 TechPostRepositoryImpl 미러)."""
        return (
            self.db.query(
                TechPost,
                func.coalesce(TechPostCommentCount.comment_count, 0),
                func.coalesce(TechPostLikeCount.like_count, 0),
                func.coalesce(TechPostViewCount.view_count, 0),
            )
            .outerjoin(TechPostCommentCount, TechPost.id == TechPostCommentCount.post_id)
            .outerjoin(TechPostLikeCount, TechPost.id == TechPostLikeCount.post_id)
            .outerjoin(TechPostViewCount, TechPost.id == TechPostViewCount.post_id)
        )

    @staticmethod
    def _to_with_counts(rows) -> list[TechPostWithCounts]:
        """(TechPost, coalesce 댓글 수, coalesce 좋아요 수, coalesce 조회 수) row → 프로젝션 변환. 목록 조회들이 공유."""
        return [
            TechPostWithCounts(
                post=post, comment_count=comment_count, like_count=like_count, view_count=view_count
            )
            for post, comment_count, like_count, view_count in rows
        ]

    def find_post_with_counts(self, id: int) -> TechPostWithCounts | None:
        """상세 단건 + 카운트 (없는 글이면 None → 호출부 404)."""
        row = self._with_counts_query().filter(TechPost.id == id).first()
        if row is None:
            return None
        return TechPostWithCounts(
            post=row[0], comment_count=row[1], like_count=row[2], view_count=row[3]
        )

    # --- 피드 ---

    def find_posts(
        self, category_id: int | None, cursor_id: int | None, limit: int
    ) -> list[TechPostWithCounts]:
        """테크 피드 (id desc), cursor(keyset) 페이지네이션 — 실사용. cursor_id보다 과거 글부터 limit개."""
        query = self._feed_query(self._with_counts_query(), category_id)
        # 커서(선택): id가 곧 작성순이므로 id < cursor_id면 더 과거 글
        if cursor_id is not None:
            query = query.filter(TechPost.id < cursor_id)
        return self._to_with_counts(query.order_by(TechPost.id.desc()).limit(limit).all())

    def find_posts_offset(
        self, category_id: int | None, offset: int, limit: int
    ) -> list[TechPostWithCounts]:
        """테크 피드 (id desc), offset 페이지네이션 — 학습용. cursor와 동일 조건 + offset/limit."""
        query = self._feed_query(self._with_counts_query(), category_id)
        return self._to_with_counts(
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
    ) -> list[TechPostWithCounts]:
        """내 글 (id desc), cursor 페이지네이션 — 실사용(무한스크롤)."""
        query = self._my_posts_query(self._with_counts_query(), user_id)
        if cursor_id is not None:
            query = query.filter(TechPost.id < cursor_id)
        return self._to_with_counts(query.order_by(TechPost.id.desc()).limit(limit).all())

    def find_my_posts_by_offset(
        self, user_id: int, offset: int, limit: int
    ) -> list[TechPostWithCounts]:
        """내 글 (id desc), offset 페이지네이션 — 학습용."""
        query = self._my_posts_query(self._with_counts_query(), user_id)
        return self._to_with_counts(
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


class TechPostLikeRepository:
    """tech_post_like 이력 접근 (api-v1 TechPostLikeRepository 미러)."""

    def __init__(self, db: Session):
        self.db = db

    def insert_ignore(self, post_id: int, uid: int) -> int:
        """좋아요 이력 삽입 (멱등). 반환값 = 실제 삽입된 행 수 —
        이미 (post_id, uid)가 있으면 INSERT IGNORE가 uk 충돌을 무시하고 0을 반환한다.
        동시 요청도 한쪽만 1을 받아 카운트 증가가 정확히 1회로 수렴.
        ORM add 후 uk 예외를 잡는 방식은 세션이 오염돼 같은 트랜잭션의 카운트 갱신이 깨지므로
        네이티브 한 문장(prefix IGNORE)으로 처리."""
        stmt = mysql_insert(TechPostLike).values(post_id=post_id, uid=uid, created_at=func.now())
        stmt = stmt.prefix_with("IGNORE")
        result = self.db.execute(stmt)
        return result.rowcount

    def delete_by_post_id_and_uid(self, post_id: int, uid: int) -> int:
        """좋아요 이력 삭제 (멱등). 반환값 = 실제 삭제된 행 수 —
        안 누른 상태면 0 (호출부가 카운트 감소를 건너뛴다). 벌크 delete 한 문장."""
        return (
            self.db.query(TechPostLike)
            .filter(TechPostLike.post_id == post_id, TechPostLike.uid == uid)
            .delete(synchronize_session=False)
        )

    def exists_by_post_id_and_uid(self, post_id: int, uid: int) -> bool:
        """likedByMe 단건 (상세 조회용) — 로그인 유저가 이 글에 좋아요를 눌렀는지."""
        return (
            self.db.query(TechPostLike.id)
            .filter(TechPostLike.post_id == post_id, TechPostLike.uid == uid)
            .first()
            is not None
        )

    def find_liked_post_ids(self, uid: int, post_ids: list[int]) -> set[int]:
        """likedByMe 배치 (목록 조회용) — 로그인 유저가 누른 글의 post_id Set (N+1 회피)."""
        if not post_ids:
            return set()
        rows = (
            self.db.query(TechPostLike.post_id)
            .filter(TechPostLike.uid == uid, TechPostLike.post_id.in_(post_ids))
            .all()
        )
        return {row[0] for row in rows}


class TechPostLikeCountRepository:
    """tech_post_like_count 원자 갱신 전용 (TechPostCommentCountRepository 미러)."""

    def __init__(self, db: Session):
        self.db = db

    def increase(self, post_id: int) -> None:
        """좋아요 수 +1 (좋아요 시). row가 없으면 1로 생성 — MySQL upsert(ON DUPLICATE KEY UPDATE)로
        "읽고 +1 후 저장"의 동시 요청 레이스 없이 DB에서 원자 갱신."""
        stmt = mysql_insert(TechPostLikeCount).values(post_id=post_id, like_count=1)
        stmt = stmt.on_duplicate_key_update(like_count=TechPostLikeCount.like_count + 1)
        self.db.execute(stmt)

    def decrease(self, post_id: int) -> None:
        """좋아요 수 -1 (좋아요 취소 시). like_count > 0 조건으로 음수 방어 —
        0이거나 row가 없으면 0건 갱신으로 무해(0에서 더 내려가지 않고, row도 만들지 않는다)."""
        (
            self.db.query(TechPostLikeCount)
            .filter(TechPostLikeCount.post_id == post_id, TechPostLikeCount.like_count > 0)
            .update(
                {TechPostLikeCount.like_count: TechPostLikeCount.like_count - 1},
                synchronize_session=False,
            )
        )
