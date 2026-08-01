from sqlalchemy import BigInteger, Boolean, Column, DateTime, Integer, String, func

from app.config.database import Base


class TechCategory(Base):
    """테크 카테고리 마스터 (게시판·뉴스 공용). 섹션은 테이블명(tech_category)이 담당 — section 컬럼 없음."""

    __tablename__ = "tech_category"

    id = Column(BigInteger, primary_key=True, autoincrement=True)
    name = Column(String(50), nullable=False)
    sort_order = Column(Integer, nullable=False, default=0)
    is_active = Column(Boolean, nullable=False, default=True)
    created_at = Column(DateTime, nullable=False, default=func.now())
