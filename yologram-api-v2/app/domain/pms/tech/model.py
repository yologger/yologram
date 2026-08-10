from dataclasses import dataclass

from sqlalchemy import BigInteger, Column, DateTime, Integer, String, Text, UniqueConstraint, func

from app.config.database import Base


class TechPost(Base):
    """테크 게시판 게시글. 섹션은 테이블명(tech_post)이 담당 — section 컬럼 없음."""

    __tablename__ = "tech_post"

    id = Column(BigInteger, primary_key=True, autoincrement=True)
    user_id = Column(BigInteger, nullable=False)  # ums 경계 넘음 → FK 없이 인덱스
    title = Column(String(200), nullable=True)
    content = Column(Text, nullable=False)
    like_count = Column(Integer, nullable=False, default=0)
    comment_count = Column(Integer, nullable=False, default=0)
    created_at = Column(DateTime, nullable=False, default=func.now())
    modified_date = Column(DateTime, nullable=False, default=func.now(), onupdate=func.now())


class TechPostCategoryMapping(Base):
    __tablename__ = "tech_post_category_mapping"
    __table_args__ = (UniqueConstraint("post_id", "category_id", name="uk_tech_post_category"),)

    id = Column(BigInteger, primary_key=True, autoincrement=True)
    post_id = Column(BigInteger, nullable=False)  # tech post 내부
    category_id = Column(BigInteger, nullable=False)  # category 경계 넘음 → FK 없이 인덱스


class TechPostCommentCount(Base):
    """테크 게시글 댓글 수 — 게시글의 비정규화 속성이라 pms 소유 (댓글 도메인은 PmsApiClient로 갱신 위임).
    PK는 게시글 id 그대로 사용(autoincrement 없음), FK 없이 컬럼만 — comment/pms 도메인 경계.
    테이블은 이미 prod에 존재하므로 매핑만 추가 (DDL 없음, api-v1 TechPostCommentCount 미러).
    갱신은 TechPostCommentCountRepository의 원자 쿼리(increase/decrease)로만 수행 —
    엔티티를 읽어 +1 후 저장하는 방식은 동시 요청 레이스가 있어 금지.
    count가 0이어도 row는 삭제하지 않는다 (조회 outerjoin+coalesce가 0을 처리, 삭제/재생성 churn 제거)."""

    __tablename__ = "tech_post_comment_count"

    post_id = Column(BigInteger, primary_key=True, autoincrement=False)
    comment_count = Column(BigInteger, nullable=False, default=0)


@dataclass
class TechPostWithCommentCount:
    """게시글 + 댓글 수 조회 프로젝션 (리포지토리 조회 결과용, 응답 스키마 아님).
    comment_count는 tech_post_comment_count outerjoin + coalesce(0) 결과 — count row가 없는 글은 0."""

    post: TechPost
    comment_count: int
