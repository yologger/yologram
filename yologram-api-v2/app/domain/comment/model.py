from sqlalchemy import BigInteger, Column, DateTime, String, func

from app.config.database import Base


class Comment(Base):
    __tablename__ = "post_comment"

    id = Column(BigInteger, primary_key=True, autoincrement=True)
    post_id = Column(BigInteger, nullable=False)  # pms 경계 넘음 → FK 없이 인덱스(DDL 관리)
    user_id = Column(BigInteger, nullable=False)  # ums 경계 넘음 → FK 없이 인덱스(DDL 관리)
    content = Column(String(1000), nullable=False)
    created_at = Column(DateTime, nullable=False, default=func.now())
    modified_date = Column(DateTime, nullable=False, default=func.now(), onupdate=func.now())
