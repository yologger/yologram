from sqlalchemy import BigInteger, Column, DateTime, Enum, Integer, String, Text, func

from app.config.database import Base
from app.domain.cms.enum import Section


class Post(Base):
    __tablename__ = "community_posts"

    id = Column(BigInteger, primary_key=True, autoincrement=True)
    section = Column(Enum(Section), nullable=False)
    user_id = Column(BigInteger, nullable=False)  # ums 경계 넘음 → FK 없이 인덱스
    title = Column(String(200), nullable=True)
    content = Column(Text, nullable=False)
    like_count = Column(Integer, nullable=False, default=0)
    comment_count = Column(Integer, nullable=False, default=0)
    created_at = Column(DateTime, nullable=False, default=func.now())
    modified_date = Column(DateTime, nullable=False, default=func.now(), onupdate=func.now())


class PostCategory(Base):
    __tablename__ = "post_categories"

    id = Column(BigInteger, primary_key=True, autoincrement=True)
    post_id = Column(BigInteger, nullable=False)  # pms 내부
    category_id = Column(BigInteger, nullable=False)  # cms 경계 넘음 → FK 없이 인덱스
