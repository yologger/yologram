from sqlalchemy.orm import Session

from app.domain.tech.category.model import TechPostCategory


class TechPostCategoryRepository:

    def __init__(self, db: Session):
        self.db = db

    def find_active(self) -> list[TechPostCategory]:
        return (
            self.db.query(TechPostCategory)
            .filter(TechPostCategory.is_active.is_(True))
            .order_by(TechPostCategory.sort_order.asc())
            .all()
        )

    def count_active_by_ids(self, ids: list[int]) -> int:
        if not ids:
            return 0
        return (
            self.db.query(TechPostCategory)
            .filter(
                TechPostCategory.id.in_(ids),
                TechPostCategory.is_active.is_(True),
            )
            .count()
        )
