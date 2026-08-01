from sqlalchemy.orm import Session

from app.domain.cms.tech.model import TechCategory


class TechCategoryRepository:

    def __init__(self, db: Session):
        self.db = db

    def find_active(self) -> list[TechCategory]:
        return (
            self.db.query(TechCategory)
            .filter(TechCategory.is_active.is_(True))
            .order_by(TechCategory.sort_order.asc())
            .all()
        )

    def find_by_ids(self, ids: list[int]) -> list[TechCategory]:
        """id 배치 조회 (뉴스 카테고리 라벨 해석용 — api-v1 findAllById 대응, is_active 무관)"""
        if not ids:
            return []
        return self.db.query(TechCategory).filter(TechCategory.id.in_(ids)).all()

    def count_active_by_ids(self, ids: list[int]) -> int:
        if not ids:
            return 0
        return (
            self.db.query(TechCategory)
            .filter(
                TechCategory.id.in_(ids),
                TechCategory.is_active.is_(True),
            )
            .count()
        )
