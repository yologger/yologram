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
