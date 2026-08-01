from typing import Protocol

from sqlalchemy.orm import Session

from app.domain.ums.repository import UserRepository


class UserQueryClient(Protocol):
    """
    tech comment → ums 도메인 경계 호출 추상화 (작성자 정보 조회).
    모놀리식에서는 ums 리포지토리를 직접 호출(LocalUserQueryClient),
    MSA 분리 시 user-api HTTP 호출 구현으로 교체한다.
    """

    def find_nicknames(self, uids: list[int]) -> dict[int, str]:
        """uid 목록의 닉네임 일괄 조회 (N+1 회피). uid→nickname. 없는 uid는 제외."""
        ...


class LocalUserQueryClient:

    def __init__(self, db: Session):
        self.repository = UserRepository(db)

    def find_nicknames(self, uids: list[int]) -> dict[int, str]:
        if not uids:
            return {}
        return {user.id: user.nickname for user in self.repository.find_by_ids(uids)}
