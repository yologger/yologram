from dataclasses import dataclass
from datetime import timedelta

# 키 스킴: {도메인 prefix}:v1:{엔티티}:{식별자} — api-v1 Cache와 동일 (캐시 데이터 상호 호환)
USER_PREFIX = "ums:users"


@dataclass(frozen=True)
class Cache:
    """캐시 항목 정의 — 키·TTL을 한 곳에 묶는다 (api-v1 infra/cache/Cache 미러)."""

    key: str
    ttl: timedelta

    @classmethod
    def user_nickname(cls, uid: int) -> "Cache":
        """
        유저 닉네임 캐시.
        닉네임 변경·탈퇴 시 명시적 무효화(delete_all)가 주 수단이고, TTL 1시간은 무효화 누락 대비 보험.
        """
        return cls(key=f"{USER_PREFIX}:v1:nickname:{uid}", ttl=timedelta(hours=1))
