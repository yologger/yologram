# 타 도메인 경계 클라이언트 — 타 도메인 리포지토리 import는 app/infra/client에서만 허용 (domain 간 직접 참조 금지).
# MSA 분리 시 Local 구현 대신 Rest(HTTP) 구현을 추가해 교체한다.
from typing import Protocol

from sqlalchemy.orm import Session

from app.domain.ums.repository import UserRepository
from app.infra.cache.user_nickname_cache import UserNicknameCache


class UmsApiClient(Protocol):
    """
    타 도메인(pms·comment) → ums 도메인 경계 호출 추상화 (작성자 정보 조회).
    모놀리식에서는 ums 리포지토리를 직접 호출(LocalUmsApiClient),
    MSA 분리 시 user-api HTTP 호출 구현으로 교체한다.
    """

    def find_nickname(self, uid: int) -> str | None:
        """uid의 닉네임. 없으면 None."""
        ...

    def find_nicknames(self, uids: list[int]) -> dict[int, str]:
        """uid 목록의 닉네임 일괄 조회 (N+1 회피). uid→nickname. 없는 uid는 제외."""
        ...


class LocalUmsApiClient:
    """닉네임 cache-aside(UserNicknameCache) 적용 지점 — pms·comment 소비처가 공통으로 캐시를 탄다 (api-v1 미러)."""

    def __init__(self, db: Session, nickname_cache: UserNicknameCache | None = None):
        self.repository = UserRepository(db)
        self.nickname_cache = nickname_cache or UserNicknameCache()

    def find_nickname(self, uid: int) -> str | None:
        return self.nickname_cache.get_nickname(uid, self._load_nickname)

    def find_nicknames(self, uids: list[int]) -> dict[int, str]:
        if not uids:
            return {}
        return self.nickname_cache.get_nicknames(uids, self._load_nicknames)

    def _load_nickname(self, uid: int) -> str | None:
        user = self.repository.find_by_id(uid)
        return user.nickname if user else None

    def _load_nicknames(self, uids) -> dict[int, str]:
        return {user.id: user.nickname for user in self.repository.find_by_ids(list(uids))}
