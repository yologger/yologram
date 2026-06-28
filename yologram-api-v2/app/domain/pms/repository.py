from sqlalchemy import exists, func
from sqlalchemy.orm import Query, Session

from app.domain.cms.enum import Section
from app.domain.pms.model import Post, PostCategoryMapping


class PostRepository:

    def __init__(self, db: Session):
        self.db = db

    def save(self, post: Post) -> Post:
        self.db.add(post)
        self.db.flush()
        self.db.refresh(post)
        return post

    def find_by_id(self, id: int) -> Post | None:
        return self.db.query(Post).filter(Post.id == id).first()

    # --- 섹션 피드 ---

    def find_posts_by_section(
        self, section: Section, category_id: int | None, cursor_id: int | None, limit: int
    ) -> list[Post]:
        """섹션 피드 (id desc), cursor(keyset) 페이지네이션 — 실사용. cursor_id보다 과거 글부터 limit개."""
        query = self._section_query(self.db.query(Post), section, category_id)
        # 커서(선택): id가 곧 작성순이므로 id < cursor_id면 더 과거 글
        if cursor_id is not None:
            query = query.filter(Post.id < cursor_id)
        return query.order_by(Post.id.desc()).limit(limit).all()

    def find_posts_by_section_offset(
        self, section: Section, category_id: int | None, offset: int, limit: int
    ) -> list[Post]:
        """섹션 피드 (id desc), offset 페이지네이션 — 학습용. cursor와 동일 조건 + offset/limit."""
        query = self._section_query(self.db.query(Post), section, category_id)
        return query.order_by(Post.id.desc()).offset(offset).limit(limit).all()

    def count_posts_by_section(self, section: Section, category_id: int | None) -> int:
        """섹션 피드 전체 개수 (offset totalCount용). 조건은 find_posts_by_section과 동일."""
        query = self._section_query(self.db.query(func.count(Post.id)), section, category_id)
        return query.scalar() or 0

    def _section_query(self, query: Query, section: Section, category_id: int | None) -> Query:
        """섹션(+카테고리) 동적 조건. cursor/offset/count가 공유. 카테고리 필터는 EXISTS(1:N 안전)."""
        query = query.filter(Post.section == section)
        if category_id is not None:
            query = query.filter(
                exists().where(
                    (PostCategoryMapping.post_id == Post.id) & (PostCategoryMapping.category_id == category_id)
                )
            )
        return query

    # --- 내 글 ---

    def find_my_posts_by_cursor(
        self, user_id: int, section: Section | None, cursor_id: int | None, limit: int
    ) -> list[Post]:
        """내 글 (id desc), cursor 페이지네이션 — 실사용(무한스크롤)."""
        query = self._my_posts_query(self.db.query(Post), user_id, section)
        if cursor_id is not None:
            query = query.filter(Post.id < cursor_id)
        return query.order_by(Post.id.desc()).limit(limit).all()

    def find_my_posts_by_offset(
        self, user_id: int, section: Section | None, offset: int, limit: int
    ) -> list[Post]:
        """내 글 (id desc), offset 페이지네이션 — 학습용."""
        query = self._my_posts_query(self.db.query(Post), user_id, section)
        return query.order_by(Post.id.desc()).offset(offset).limit(limit).all()

    def count_my_posts(self, user_id: int, section: Section | None) -> int:
        """내 글 전체 개수 (offset totalCount용). 조건은 find_my_posts와 동일."""
        query = self._my_posts_query(self.db.query(func.count(Post.id)), user_id, section)
        return query.scalar() or 0

    def _my_posts_query(self, query: Query, user_id: int, section: Section | None) -> Query:
        """내 글 동적 조건: user_id는 항상, section은 있을 때만 결합. cursor/offset/count가 공유."""
        query = query.filter(Post.user_id == user_id)
        if section is not None:
            query = query.filter(Post.section == section)
        return query


class PostCategoryMappingRepository:

    def __init__(self, db: Session):
        self.db = db

    def save(self, post_category: PostCategoryMapping) -> PostCategoryMapping:
        self.db.add(post_category)
        self.db.flush()
        return post_category

    def find_by_post_id(self, post_id: int) -> list[PostCategoryMapping]:
        return self.db.query(PostCategoryMapping).filter(PostCategoryMapping.post_id == post_id).all()

    def find_by_post_ids(self, post_ids: list[int]) -> list[PostCategoryMapping]:
        if not post_ids:
            return []
        return self.db.query(PostCategoryMapping).filter(PostCategoryMapping.post_id.in_(post_ids)).all()

    def delete_by_post_id(self, post_id: int) -> None:
        """게시글 수정/삭제 시 카테고리 매핑 전체 제거 (수정은 제거 후 재생성으로 교체)."""
        self.db.query(PostCategoryMapping).filter(PostCategoryMapping.post_id == post_id).delete()
