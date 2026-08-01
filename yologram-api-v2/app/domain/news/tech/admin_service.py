from sqlalchemy.orm import Session

from app.core.exception import TechNewsSourceDuplicateException, TechNewsSourceNotFoundException
from app.domain.news.tech.admin_schema import (
    AdminTechNewsSourceCreateRequest,
    AdminTechNewsSourceResponse,
    AdminTechNewsSourceUpdateRequest,
)
from app.domain.news.tech.model import TechNewsSource
from app.domain.news.tech.repository import TechNewsSourceRepository


class AdminTechNewsSourceService:
    """어드민 테크 뉴스 소스 CRUD (api-v1 AdminTechNewsSourceService 미러)"""

    def __init__(self, db: Session):
        self.repository = TechNewsSourceRepository(db)

    def get_sources(self) -> list[AdminTechNewsSourceResponse]:
        return [
            AdminTechNewsSourceResponse.from_source(source)
            for source in self.repository.find_all_order_by_id_asc()
        ]

    def create(self, request: AdminTechNewsSourceCreateRequest) -> AdminTechNewsSourceResponse:
        if self.repository.exists_by_url(request.url):
            raise TechNewsSourceDuplicateException()

        saved = self.repository.save(
            TechNewsSource(
                name=request.name,
                url=request.url,
                is_active=request.is_active,
            )
        )
        return AdminTechNewsSourceResponse.from_source(saved)

    def update(
        self, id: int, request: AdminTechNewsSourceUpdateRequest
    ) -> AdminTechNewsSourceResponse:
        source = self.repository.find_by_id(id)
        if source is None:
            raise TechNewsSourceNotFoundException()

        if request.url is not None:
            # 자기 자신 제외 중복 검사 — 같은 url로의 갱신(무변경)은 허용
            if self.repository.exists_by_url_and_id_not(request.url, id):
                raise TechNewsSourceDuplicateException()
            source.url = request.url
        if request.name is not None:
            source.name = request.name
        if request.is_active is not None:
            source.is_active = request.is_active

        # modified_date(onupdate)가 응답에 반영되도록 즉시 flush — api-v1 saveAndFlush 미러
        saved = self.repository.save(source)
        return AdminTechNewsSourceResponse.from_source(saved)

    def delete(self, id: int) -> None:
        """
        hard delete — 수집 중지는 is_active=false가 담당하고, 삭제는 목록에서 완전 제거.
        tech_news가 source_id를 무FK로 참조하지만 뉴스 표시는 비정규화된 source_name 스냅샷만
        사용(TechNews·TechNewsResponse)이라 소스를 지워도 기존 뉴스 노출에 영향 없음 (api-v1 동일 근거).
        """
        source = self.repository.find_by_id(id)
        if source is None:
            raise TechNewsSourceNotFoundException()
        self.repository.delete(source)
