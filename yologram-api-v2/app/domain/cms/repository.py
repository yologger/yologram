from sqlalchemy.orm import Session

from app.domain.cms.enum import Section
from app.domain.cms.model import Category


class CategoryRepository:

    def __init__(self, db: Session):
        self.db = db

    def find_active_by_section(self, section: Section) -> list[Category]:
        return self.db.query(Category).filter(
            Category.section == section,
            Category.is_active.is_(True),
        ).order_by(Category.sort_order.asc()).all()

    def count_active_by_section_and_ids(self, section: Section, ids: list[int]) -> int:
        if not ids:
            return 0
        return self.db.query(Category).filter(
            Category.id.in_(ids),
            Category.section == section,
            Category.is_active.is_(True),
        ).count()
