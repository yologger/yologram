# 타 도메인 경계 클라이언트 — 타 도메인 리포지토리 import는 app/infra/client에서만 허용 (domain 간 직접 참조 금지).
# MSA 분리 시 Local 구현 대신 Rest(HTTP) 구현을 추가해 교체한다.
from collections.abc import Collection
from typing import Protocol

from sqlalchemy.orm import Session

from app.domain.cms.tech.repository import TechCategoryRepository


class CmsApiClient(Protocol):
    """
    타 도메인(pms·news) → tech category 도메인 경계 호출 추상화.
    모놀리식에서는 category 리포지토리를 직접 호출(LocalCmsApiClient),
    MSA 분리 시 category-api HTTP 호출 구현으로 교체한다.
    """

    def all_active(self, category_ids: Collection[int]) -> bool:
        """category_ids가 모두 테크 게시판의 활성 카테고리이면 True. 빈 목록은 True."""
        ...

    def find_category_names(self, category_ids: Collection[int]) -> dict[int, str]:
        """카테고리 라벨 일괄 조회 (N+1 회피). id→name, 삭제된 id는 결과에서 제외."""
        ...


class LocalCmsApiClient:

    def __init__(self, db: Session):
        self.repository = TechCategoryRepository(db)

    def all_active(self, category_ids: Collection[int]) -> bool:
        distinct_ids = list(set(category_ids))
        if not distinct_ids:
            return True
        matched = self.repository.count_active_by_ids(distinct_ids)
        return matched == len(distinct_ids)

    def find_category_names(self, category_ids: Collection[int]) -> dict[int, str]:
        distinct_ids = list(set(category_ids))
        if not distinct_ids:
            return {}
        # 라벨 해석은 is_active 무관 (api-v1 findAllById 정합) — 삭제된 id는 조회 결과에 없어 자연 제외
        return {c.id: c.name for c in self.repository.find_by_ids(distinct_ids)}
