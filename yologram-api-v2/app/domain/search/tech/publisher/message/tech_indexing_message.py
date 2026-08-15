from dataclasses import dataclass

# 인덱싱 대상 구분 — worker가 이 값으로 분기한다. 큐를 대상별로 나누지 않고 이 필드로 가른다
TARGET_TECH_POST = "TECH_POST"


@dataclass(frozen=True)
class TechIndexingMessage:
    """
    게시글 인덱싱 작업 메시지 (api-v1 TechIndexingMessage·worker 구독 계약과 문자열로 미러).

    단건도 from == to로 보내 범위 인덱싱과 같은 경로를 탄다.
    필드명(target·from·to)이 세 프로젝트의 계약이라 한쪽만 바꾸면 소비가 깨진다.
    """

    from_id: int
    to_id: int
    target: str = TARGET_TECH_POST

    def to_payload(self) -> dict:
        # from은 파이썬 예약어라 필드명을 from_id로 두고 직렬화 시점에 계약 이름으로 바꾼다
        return {"target": self.target, "from": self.from_id, "to": self.to_id}
