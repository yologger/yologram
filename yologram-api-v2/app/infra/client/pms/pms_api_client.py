# 타 도메인 경계 클라이언트 — 타 도메인 리포지토리 import는 app/infra/client에서만 허용 (domain 간 직접 참조 금지).
# MSA 분리 시 Local 구현 대신 Rest(HTTP) 구현을 추가해 교체한다.
from typing import Protocol

from sqlalchemy.orm import Session

from app.domain.pms.tech.repository import TechPostRepository


class PmsApiClient(Protocol):
    """
    tech comment → tech post 도메인 경계 호출 추상화 (대상 글 존재 검증).
    모놀리식에서는 tech post 리포지토리를 직접 호출(LocalPmsApiClient),
    MSA 분리 시 post-api HTTP 호출 구현으로 교체한다.
    """

    def exists(self, post_id: int) -> bool:
        """post_id의 게시글이 존재하면 True."""
        ...


class LocalPmsApiClient:

    def __init__(self, db: Session):
        self.repository = TechPostRepository(db)

    def exists(self, post_id: int) -> bool:
        return self.repository.find_by_id(post_id) is not None
