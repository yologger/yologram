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
