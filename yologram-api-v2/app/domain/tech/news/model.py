import enum

from sqlalchemy import BigInteger, Column, DateTime, String, Text, func

from app.config.database import Base


class TechArticleStatus(str, enum.Enum):
    """테크 아티클 파이프라인 상태 (worker가 관리). 공개 조회는 SUMMARIZED만 노출"""

    COLLECTED = "COLLECTED"
    SUMMARIZED = "SUMMARIZED"
    FAILED = "FAILED"


class TechArticle(Base):
    """
    테크 아티클 — worker가 수집·요약해 쌓는 tech_article 테이블의 조회 전용 매핑.
    api-v2는 읽기만 하므로 조회에 필요한 컬럼만 매핑 (쓰기·상태 전이는 worker 소관).
    """

    __tablename__ = "tech_article"

    id = Column(BigInteger, primary_key=True, autoincrement=True)
    source_id = Column(BigInteger, nullable=False)
    title = Column(String(500), nullable=False)
    link = Column(String(500), nullable=False)
    # LLM 한국어 요약 (COLLECTED 단계에선 null)
    summary = Column(Text, nullable=True)
    source_name = Column(String(100), nullable=False)
    published_at = Column(DateTime, nullable=False)
    status = Column(String(20), nullable=False)
    created_at = Column(DateTime, nullable=False, default=func.now())


class TechArticleCategoryMapping(Base):
    """아티클 ↔ 카테고리 N:M (tech_category.id 참조 — worker가 LLM 분류로 채움, api-v2는 조회 전용)"""

    __tablename__ = "tech_article_category_mapping"

    id = Column(BigInteger, primary_key=True, autoincrement=True)
    article_id = Column(BigInteger, nullable=False)
    # 카테고리 마스터 tech_category.id (무FK — 라벨은 조회 시 조인 해석)
    category_id = Column(BigInteger, nullable=False)
