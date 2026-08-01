from collections.abc import Collection
from typing import Protocol

from sqlalchemy.orm import Session

from app.domain.tech.category.repository import TechCategoryRepository


class TechPostCategoryQueryClient(Protocol):
    """
    tech post → tech category 도메인 경계 호출 추상화.
    모놀리식에서는 category 리포지토리를 직접 호출(LocalTechPostCategoryQueryClient),
    MSA 분리 시 category-api HTTP 호출 구현으로 교체한다.
    """

    def all_active(self, category_ids: Collection[int]) -> bool:
        """category_ids가 모두 테크 게시판의 활성 카테고리이면 True. 빈 목록은 True."""
        ...


class LocalTechPostCategoryQueryClient:

    def __init__(self, db: Session):
        self.repository = TechCategoryRepository(db)

    def all_active(self, category_ids: Collection[int]) -> bool:
        distinct_ids = list(set(category_ids))
        if not distinct_ids:
            return True
        matched = self.repository.count_active_by_ids(distinct_ids)
        return matched == len(distinct_ids)
