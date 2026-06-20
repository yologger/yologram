from sqlalchemy import BigInteger, Boolean, Column, DateTime, Enum, Integer, String, func

from app.config.database import Base
from app.domain.cms.enum import Section


class PostCategory(Base):
    __tablename__ = "post_category"

    id = Column(BigInteger, primary_key=True, autoincrement=True)
    section = Column(Enum(Section), nullable=False)
    name = Column(String(50), nullable=False)
    sort_order = Column(Integer, nullable=False, default=0)
    is_active = Column(Boolean, nullable=False, default=True)
    created_at = Column(DateTime, nullable=False, default=func.now())
