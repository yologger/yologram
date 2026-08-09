from dataclasses import dataclass
from datetime import timedelta

# 키 스킴: {도메인 prefix}:v1:{엔티티}:{식별자} — api-v1 Cache와 동일 (캐시 데이터 상호 호환)
USER_PREFIX = "ums:users"
NEWS_PREFIX = "news:tech"


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

    @classmethod
    def tech_news_first_page(cls, category_id: int | None, size: int) -> "Cache":
        """
        테크 뉴스 첫 페이지 캐시 (cursor 없는 요청 전용 — 트래픽 대부분이 첫 페이지).
        무효화는 worker가 요약 완료 시 키 전수 열거 UNLINK — 키 스킴·size 상한(1~50)은
        worker TechNewsFirstPageCacheInvalidator와의 문자열 계약. TTL 3분은 삭제 누락·
        레이스(삭제 직후 옛 목록 SET 부활) 대비 보험이자 낡음의 상한.
        category_id 미지정은 "all", size는 정규화(1~MAX) 후 전달해 키 폭증을 막는다.
        """
        category = category_id if category_id is not None else "all"
        return cls(
            key=f"{NEWS_PREFIX}:v1:first-page:{category}:{size}",
            ttl=timedelta(minutes=3),
        )
