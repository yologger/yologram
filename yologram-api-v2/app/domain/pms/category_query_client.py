from collections.abc import Collection
from typing import Protocol

from sqlalchemy.orm import Session

from app.domain.cms.enum import Section
from app.domain.cms.repository import CategoryRepository


class CategoryQueryClient(Protocol):
    """
    pms → cms 도메인 경계 호출 추상화.
    모놀리식에서는 cms 리포지토리를 직접 호출(LocalCategoryQueryClient),
    MSA 분리 시 cms-api HTTP 호출 구현으로 교체한다.
    """

    def all_active_in_section(self, section: Section, category_ids: Collection[int]) -> bool:
        """category_ids가 모두 해당 section의 활성 카테고리이면 True. 빈 목록은 True."""
        ...


class LocalCategoryQueryClient:

    def __init__(self, db: Session):
        self.repository = CategoryRepository(db)

    def all_active_in_section(self, section: Section, category_ids: Collection[int]) -> bool:
        distinct_ids = list(set(category_ids))
        if not distinct_ids:
            return True
        matched = self.repository.count_active_by_section_and_ids(section, distinct_ids)
        return matched == len(distinct_ids)
