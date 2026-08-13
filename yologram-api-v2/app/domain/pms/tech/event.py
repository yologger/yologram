from dataclasses import dataclass, field
from datetime import datetime

EVENT_TYPE_POST_VIEW = "POST_VIEW"
SECTION_TECH = "TECH"


@dataclass
class PostViewEvent:
    """
    게시글 조회 이벤트 (Kinesis 발행 페이로드, 필드 6개 고정 — api-v1 PostViewEvent 미러).
    상세 조회가 성공한 뒤에만 발행하고, 중복 판정(dedup)은 소비 쪽(worker)이 담당 — producer는 원본만 보낸다.

    예: {"eventType":"POST_VIEW","section":"TECH","postId":1200,"uid":12,"ip":"1.2.3.4","occurredAt":"2026-08-12T21:30:00"}
    """

    post_id: int

    # 조회한 유저 — 비로그인이면 None (선택 인증 get_optional_authenticated_user 재사용)
    uid: int | None = None

    # 클라이언트 IP — X-Forwarded-For 첫 값, 없으면 접속 주소 폴백 (resolve_client_ip)
    ip: str | None = None

    # 발생 시각 — 기존 직렬화 규약(datetime ISO)과 동일하게 초 단위, 타임존 오프셋 없음
    occurred_at: datetime = field(default_factory=lambda: datetime.now().replace(microsecond=0))

    # 이벤트 종류 — 소비 쪽 분기 키 (고정)
    event_type: str = EVENT_TYPE_POST_VIEW

    # 섹션 — 테이블·경로가 섹션을 담당하므로 응답 스키마와 동일하게 "TECH" 고정 문자열
    section: str = SECTION_TECH

    def to_payload(self) -> dict:
        """발행 페이로드(dict) — 필드명·개수·순서가 api-v1 Jackson 직렬화와 같아야 한다
        (같은 스트림 yologram-post-view-event-prod를 worker 하나가 소비하므로 계약이 100% 동일해야 함).
        occurredAt은 초 단위 ISO 문자열(마이크로초·오프셋 없음) — v1 LocalDateTime 직렬화와 일치."""
        return {
            "eventType": self.event_type,
            "section": self.section,
            "postId": self.post_id,
            "uid": self.uid,
            "ip": self.ip,
            "occurredAt": self.occurred_at.replace(microsecond=0).isoformat(),
        }
