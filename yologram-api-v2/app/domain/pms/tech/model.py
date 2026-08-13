from dataclasses import dataclass

from sqlalchemy import BigInteger, Column, DateTime, String, Text, UniqueConstraint, func

from app.config.database import Base


class TechPost(Base):
    """테크 게시판 게시글. 섹션은 테이블명(tech_post)이 담당 — section 컬럼 없음.
    카운트(댓글 수·좋아요 수)는 별도 1:1 테이블(tech_post_comment_count·tech_post_like_count) 소관 —
    사장된 like_count·comment_count 컬럼은 매핑 제거(prod 컬럼 DEFAULT 0이라 INSERT 안전, drop DDL은 배포 후)."""

    __tablename__ = "tech_post"

    id = Column(BigInteger, primary_key=True, autoincrement=True)
    user_id = Column(BigInteger, nullable=False)  # ums 경계 넘음 → FK 없이 인덱스
    title = Column(String(200), nullable=True)
    content = Column(Text, nullable=False)
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


class TechPostLike(Base):
    """테크 게시글 좋아요 이력 — "누가 어떤 글에 좋아요를 눌렀나"의 진실(source of truth).
    UNIQUE(post_id, uid)로 유저당 글당 1개 보장 — 동시 요청 uk 충돌은 no-op으로 수렴(멱등).
    카운트(tech_post_like_count)는 이 이력의 비정규화 — 불일치 시 이력 기준 재계산 복구.
    삽입은 TechPostLikeRepository.insert_ignore(INSERT IGNORE)로만 — 세션 예외 오염 없이 한 문장 멱등
    (api-v1 TechPostLike 미러)."""

    __tablename__ = "tech_post_like"
    __table_args__ = (UniqueConstraint("post_id", "uid", name="uk_tech_post_like"),)

    id = Column(BigInteger, primary_key=True, autoincrement=True)
    post_id = Column(BigInteger, nullable=False)  # 대상 게시글 (무FK 관례, uk가 인덱스 겸용)
    uid = Column(BigInteger, nullable=False)  # 누른 유저 (ums 경계 넘음 → FK 없음)
    created_at = Column(DateTime, nullable=False, default=func.now())


class TechPostLikeCount(Base):
    """테크 게시글 좋아요 수 — pms 소유 비정규화 (TechPostCommentCount 미러).
    이력(tech_post_like)이 진실, 이 테이블은 표시용 캐시 — 불일치 시 이력 COUNT로 재계산 복구.
    갱신은 TechPostLikeCountRepository의 원자 쿼리(increase/decrease)로만.
    count가 0이어도 row는 삭제하지 않는다 (조회 outerjoin+coalesce가 0을 처리)."""

    __tablename__ = "tech_post_like_count"

    post_id = Column(BigInteger, primary_key=True, autoincrement=False)
    like_count = Column(BigInteger, nullable=False, default=0)


@dataclass
class TechPostWithCounts:
    """게시글 + 카운트(댓글 수·좋아요 수) 조회 프로젝션 (리포지토리 조회 결과용, 응답 스키마 아님).
    각 카운트는 tech_post_comment_count / tech_post_like_count outerjoin + coalesce(0) 결과 —
    count row가 없는 글은 0. liked_by_me는 개인화 값이라 프로젝션이 아닌 service에서 이력 배치 조회."""

    post: TechPost
    comment_count: int
    like_count: int
