from sqlalchemy import exists
from sqlalchemy.orm import Session

from app.domain.news.tech.cursor import TechNewsCursor
from app.domain.news.tech.model import TechNews, TechNewsCategoryMapping, TechNewsStatus


class TechNewsRepository:

    def __init__(self, db: Session):
        self.db = db

    def find_summarized_news(
        self, category_id: int | None, cursor: TechNewsCursor | None, limit: int
    ) -> list[TechNews]:
        """발행순(published_at desc, id desc) keyset 조회 — SUMMARIZED만, category_id는 있을 때만 필터."""
        # 요약 완료된 뉴스만 노출 (COLLECTED는 요약 대기, FAILED는 요약 불가 — 화면 제외 결정)
        query = self.db.query(TechNews).filter(TechNews.status == TechNewsStatus.SUMMARIZED.value)

        # 카테고리 필터: 매핑에 해당 categoryId가 있는 글만 — EXISTS라 글:카테고리 1:N에서도 행 불어남 없음
        # (게시판 카테고리 필터와 동일 패턴, idx (category_id, news_id) 커버)
        if category_id is not None:
            query = query.filter(
                exists().where(
                    (TechNewsCategoryMapping.news_id == TechNews.id)
                    & (TechNewsCategoryMapping.category_id == category_id)
                )
            )

        # (published_at, id) 복합 keyset: 발행 시각이 더 과거이거나, 같은 시각이면 id가 더 작은 글부터.
        # 동일 발행 시각 다건(AWS What's New 등)의 페이지 경계 누락·중복을 id tie-breaker로 방지
        if cursor is not None:
            query = query.filter(
                (TechNews.published_at < cursor.published_at)
                | ((TechNews.published_at == cursor.published_at) & (TechNews.id < cursor.id))
            )

        # 정렬·탐색 모두 idx_tech_news_published_at_id(published_at, id)를 탄다
        return query.order_by(TechNews.published_at.desc(), TechNews.id.desc()).limit(limit).all()


class TechNewsCategoryMappingRepository:

    def __init__(self, db: Session):
        self.db = db

    def find_by_news_ids(self, news_ids: list[int]) -> list[TechNewsCategoryMapping]:
        """목록 응답용 배치 조회 (N+1 회피 — 게시판 find_by_post_ids 패턴)"""
        if not news_ids:
            return []
        return (
            self.db.query(TechNewsCategoryMapping)
            .filter(TechNewsCategoryMapping.news_id.in_(news_ids))
            .all()
        )
