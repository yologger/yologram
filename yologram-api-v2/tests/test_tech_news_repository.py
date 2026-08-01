import itertools
from datetime import datetime, timedelta

import pytest
from sqlalchemy import create_engine
from sqlalchemy.orm import sessionmaker
from testcontainers.mysql import MySqlContainer

from app.config.database import Base
from app.domain.cms.tech.model import TechCategory
from app.domain.cms.tech.repository import TechCategoryRepository
from app.domain.news.tech.cursor import TechNewsCursor
from app.domain.news.tech.model import TechNews, TechNewsCategoryMapping, TechNewsStatus
from app.domain.news.tech.repository import TechNewsCategoryMappingRepository, TechNewsRepository

BASE = datetime(2026, 7, 18, 9, 0, 0)

# tech_category 마스터 시드 (매핑은 tech_category.id 참조)
BACKEND_ID = 1
CLOUD_ID = 2
FRONTEND_ID = 3
SECURITY_ID = 4
INACTIVE_ID = 5  # 비활성 카테고리 (라벨 해석은 is_active 무관 — api-v1 findAllById 정합)

_seq = itertools.count(1)


@pytest.fixture(scope="module")
def db_session():
    with MySqlContainer("mysql:8.0") as mysql:
        url = mysql.get_connection_url().replace("mysql://", "mysql+pymysql://", 1)
        engine = create_engine(url)
        Base.metadata.create_all(engine)
        Session = sessionmaker(bind=engine)
        session = Session()
        # 카테고리 마스터 시드 — 게시판·뉴스 공용 tech_category
        session.add_all(
            [
                TechCategory(id=BACKEND_ID, name="Backend", sort_order=1, is_active=True),
                TechCategory(id=CLOUD_ID, name="Cloud", sort_order=2, is_active=True),
                TechCategory(id=FRONTEND_ID, name="Frontend", sort_order=3, is_active=True),
                TechCategory(id=SECURITY_ID, name="Security", sort_order=4, is_active=True),
                TechCategory(id=INACTIVE_ID, name="Legacy", sort_order=5, is_active=False),
            ]
        )
        session.commit()
        yield session
        session.close()
        engine.dispose()


@pytest.fixture(autouse=True)
def clean(db_session):
    db_session.query(TechNews).delete()
    db_session.query(TechNewsCategoryMapping).delete()
    db_session.commit()


def _news(db_session, published_at: datetime, status: TechNewsStatus = TechNewsStatus.SUMMARIZED) -> TechNews:
    seq = next(_seq)
    news = TechNews(
        id=seq,
        source_id=1,
        title=f"제목 {seq}",
        link=f"https://a/{seq}",
        summary=f"요약 {seq}" if status == TechNewsStatus.SUMMARIZED else None,
        source_name="테크 블로그",
        published_at=published_at,
        status=status.value,
    )
    db_session.add(news)
    db_session.flush()
    return news


class TestTechNewsRepository:

    def test_SUMMARIZED만_발행순으로_반환한다(self, db_session):
        repo = TechNewsRepository(db_session)
        old = _news(db_session, BASE - timedelta(days=2))
        recent = _news(db_session, BASE)
        _news(db_session, BASE - timedelta(days=1), status=TechNewsStatus.COLLECTED)
        _news(db_session, BASE - timedelta(days=1), status=TechNewsStatus.FAILED)
        db_session.commit()

        result = repo.find_summarized_news(None, None, 10)

        assert [n.id for n in result] == [recent.id, old.id]

    def test_발행_시각이_같으면_id_내림차순으로_정렬된다(self, db_session):
        repo = TechNewsRepository(db_session)
        first = _news(db_session, BASE)
        second = _news(db_session, BASE)
        db_session.commit()

        result = repo.find_summarized_news(None, None, 10)

        assert [n.id for n in result] == [second.id, first.id]

    def test_커서_이후_페이지가_중복_누락_없이_이어진다__동일_발행_시각_경계_포함(self, db_session):
        repo = TechNewsRepository(db_session)
        # 같은 발행 시각 4건 + 다른 시각 2건 — 경계가 동일 시각 한가운데 걸리게 페이지 크기 3
        news_list = [
            _news(db_session, BASE + timedelta(hours=1)),  # 최신
            _news(db_session, BASE),
            _news(db_session, BASE),
            _news(db_session, BASE),
            _news(db_session, BASE),
            _news(db_session, BASE - timedelta(hours=1)),  # 가장 과거
        ]
        db_session.commit()

        page1 = repo.find_summarized_news(None, None, 3)
        cursor = TechNewsCursor(published_at=page1[-1].published_at, id=page1[-1].id)
        page2 = repo.find_summarized_news(None, cursor, 3)

        all_ids = [n.id for n in page1 + page2]
        assert set(all_ids) == {n.id for n in news_list}  # 누락 없음
        assert len(all_ids) == len(set(all_ids))  # 중복 없음

    def test_limit만큼만_반환한다(self, db_session):
        repo = TechNewsRepository(db_session)
        for i in range(5):
            _news(db_session, BASE + timedelta(minutes=i))
        db_session.commit()

        assert len(repo.find_summarized_news(None, None, 2)) == 2

    def test_데이터가_없으면_빈_목록을_반환한다(self, db_session):
        repo = TechNewsRepository(db_session)

        assert repo.find_summarized_news(None, None, 10) == []

    def test_categoryId_필터는_해당_매핑이_있는_글만_반환한다(self, db_session):
        repo = TechNewsRepository(db_session)
        backend = _news(db_session, BASE)
        cloud_only = _news(db_session, BASE - timedelta(hours=1))
        db_session.add_all(
            [
                TechNewsCategoryMapping(news_id=backend.id, category_id=BACKEND_ID),
                TechNewsCategoryMapping(news_id=backend.id, category_id=CLOUD_ID),
                TechNewsCategoryMapping(news_id=cloud_only.id, category_id=CLOUD_ID),
            ]
        )
        db_session.commit()

        result = repo.find_summarized_news(BACKEND_ID, None, 10)

        assert [n.id for n in result] == [backend.id]

    def test_categoryId_필터에_매칭이_없으면_빈_목록을_반환한다(self, db_session):
        repo = TechNewsRepository(db_session)
        _news(db_session, BASE)
        db_session.commit()

        assert repo.find_summarized_news(SECURITY_ID, None, 10) == []


class TestTechNewsCategoryMappingRepository:

    def test_news_ids_배치_조회(self, db_session):
        repo = TechNewsCategoryMappingRepository(db_session)
        db_session.add_all(
            [
                TechNewsCategoryMapping(news_id=101, category_id=BACKEND_ID),
                TechNewsCategoryMapping(news_id=101, category_id=CLOUD_ID),
                TechNewsCategoryMapping(news_id=102, category_id=FRONTEND_ID),
                TechNewsCategoryMapping(news_id=103, category_id=SECURITY_ID),
            ]
        )
        db_session.commit()

        result = repo.find_by_news_ids([101, 102])

        assert sorted((m.news_id, m.category_id) for m in result) == [
            (101, BACKEND_ID),
            (101, CLOUD_ID),
            (102, FRONTEND_ID),
        ]

    def test_빈_id_목록이면_쿼리_없이_빈_목록을_반환한다(self, db_session):
        repo = TechNewsCategoryMappingRepository(db_session)

        assert repo.find_by_news_ids([]) == []


class TestTechCategoryRepositoryFindByIds:
    """뉴스 카테고리 라벨 해석용 배치 조회 — tech_category 마스터 (시드 기반)"""

    def test_id_배치_조회는_비활성_카테고리도_포함한다(self, db_session):
        repo = TechCategoryRepository(db_session)

        result = repo.find_by_ids([BACKEND_ID, INACTIVE_ID, 999])  # 999는 미존재 — 결과에서 빠짐

        assert sorted((c.id, c.name) for c in result) == [(BACKEND_ID, "Backend"), (INACTIVE_ID, "Legacy")]

    def test_빈_id_목록이면_쿼리_없이_빈_목록을_반환한다(self, db_session):
        repo = TechCategoryRepository(db_session)

        assert repo.find_by_ids([]) == []
