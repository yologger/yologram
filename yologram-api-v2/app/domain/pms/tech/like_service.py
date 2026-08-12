from sqlalchemy.orm import Session

from app.core.exception import PostNotFoundException
from app.domain.pms.tech.repository import (
    TechPostLikeCountRepository,
    TechPostLikeRepository,
    TechPostRepository,
)


class TechPostLikeService:
    """테크 게시글 좋아요 — 원장(tech_post_like)이 진실, 카운트(tech_post_like_count)는 비정규화.
    레거시(BoardLikeService) 개선 3가지 (api-v1 TechPostLikeService 미러):
    ①원자 쿼리 증감(엔티티 읽고 +1 금지) ②멱등(중복 좋아요 POST·미좋아요 DELETE는 no-op 200 —
    더블클릭·재시도 안전, 레거시는 409) ③count 0이어도 row 유지(조회 coalesce가 없음=0 처리)."""

    def __init__(self, db: Session):
        self.post_repository = TechPostRepository(db)
        self.like_repository = TechPostLikeRepository(db)
        self.like_count_repository = TechPostLikeCountRepository(db)

    def like(self, post_id: int, uid: int) -> None:
        """좋아요 (멱등). 원장 INSERT IGNORE가 실제로 삽입한 경우에만 카운트 +1 —
        이미 누른 상태(동시 요청 uk 충돌 포함)는 0행 삽입 → 카운트 미증가로 원장과 정합 유지.
        같은 세션(get_db 요청 단위 commit)이라 원장 삽입과 카운트 증가가 원자적으로 커밋/롤백된다."""
        # 없는 글이면 404 (고아 좋아요 방지 — 댓글 작성의 exists 검증과 동일 규칙)
        if self.post_repository.find_by_id(post_id) is None:
            raise PostNotFoundException()

        inserted = self.like_repository.insert_ignore(post_id, uid)
        if inserted > 0:
            self.like_count_repository.increase(post_id)

    def unlike(self, post_id: int, uid: int) -> None:
        """좋아요 취소 (멱등). 원장 DELETE가 실제로 지운 경우에만 카운트 -1 —
        안 누른 상태는 0행 삭제 → no-op (0 미만 방어는 카운트 쿼리에서 한 번 더)."""
        # 없는 글이면 404 (좋아요와 대칭 — 잘못된 대상 호출을 조용히 삼키지 않는다)
        if self.post_repository.find_by_id(post_id) is None:
            raise PostNotFoundException()

        deleted = self.like_repository.delete_by_post_id_and_uid(post_id, uid)
        if deleted > 0:
            self.like_count_repository.decrease(post_id)
