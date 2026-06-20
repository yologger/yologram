from sqlalchemy.orm import Session

from app.domain.cms.enum import Section
from app.domain.cms.model import PostCategory


class PostCategoryRepository:

    def __init__(self, db: Session):
        self.db = db

    def find_active_by_section(self, section: Section) -> list[PostCategory]:
        return self.db.query(PostCategory).filter(
            PostCategory.section == section,
            PostCategory.is_active.is_(True),
        ).order_by(PostCategory.sort_order.asc()).all()

    def count_active_by_section_and_ids(self, section: Section, ids: list[int]) -> int:
        if not ids:
            return 0
        return self.db.query(PostCategory).filter(
            PostCategory.id.in_(ids),
            PostCategory.section == section,
            PostCategory.is_active.is_(True),
        ).count()
