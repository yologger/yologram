import itertools

import pytest
from sqlalchemy import create_engine
from sqlalchemy.orm import sessionmaker
from testcontainers.mysql import MySqlContainer

from app.config.database import Base
from app.domain.news.tech.model import TechNewsSource
from app.domain.news.tech.repository import TechNewsSourceRepository

_seq = itertools.count(1)


@pytest.fixture(scope="module")
def db_session():
    with MySqlContainer("mysql:8.0") as mysql:
        url = mysql.get_connection_url().replace("mysql://", "mysql+pymysql://", 1)
        engine = create_engine(url)
        Base.metadata.create_all(engine)
        Session = sessionmaker(bind=engine)
        session = Session()
        yield session
        session.close()
        engine.dispose()


@pytest.fixture(autouse=True)
def clean(db_session):
    db_session.query(TechNewsSource).delete()
    db_session.commit()


def _source(db_session, url: str | None = None, **kwargs) -> TechNewsSource:
    seq = next(_seq)
    source = TechNewsSource(
        name=kwargs.pop("name", f"소스 {seq}"),
        url=url or f"https://feed.example.com/rss/{seq}",
        **kwargs,
    )
    repo = TechNewsSourceRepository(db_session)
    saved = repo.save(source)
    db_session.commit()
    return saved


class TestTechNewsSourceRepository:

    def test_저장하면_id와_타임스탬프가_채워지고_is_active_기본값은_true다(self, db_session):
        saved = _source(db_session)

        assert saved.id is not None
        assert saved.is_active is True  # 기본값
        assert saved.created_at is not None
        assert saved.modified_date is not None

    def test_is_active_false로_저장된다(self, db_session):
        saved = _source(db_session, is_active=False)

        assert saved.is_active is False

    def test_전체_목록을_id_오름차순으로_반환한다(self, db_session):
        repo = TechNewsSourceRepository(db_session)
        first = _source(db_session)
        second = _source(db_session)
        third = _source(db_session)

        result = repo.find_all_order_by_id_asc()

        assert [s.id for s in result] == sorted([first.id, second.id, third.id])

    def test_빈_테이블이면_빈_목록을_반환한다(self, db_session):
        repo = TechNewsSourceRepository(db_session)

        assert repo.find_all_order_by_id_asc() == []

    def test_find_by_id_존재하면_반환_없으면_None(self, db_session):
        repo = TechNewsSourceRepository(db_session)
        saved = _source(db_session)

        found = repo.find_by_id(saved.id)
        assert found is not None
        assert found.url == saved.url

        assert repo.find_by_id(saved.id + 999) is None

    def test_exists_by_url_등록된_url이면_true_아니면_false(self, db_session):
        repo = TechNewsSourceRepository(db_session)
        saved = _source(db_session)

        assert repo.exists_by_url(saved.url) is True
        assert repo.exists_by_url("https://none.example.com/rss") is False

    def test_exists_by_url_and_id_not_자기_자신은_제외한다(self, db_session):
        repo = TechNewsSourceRepository(db_session)
        mine = _source(db_session)
        other = _source(db_session)

        assert repo.exists_by_url_and_id_not(mine.url, mine.id) is False  # 자기 자신 제외
        assert repo.exists_by_url_and_id_not(other.url, mine.id) is True  # 타 소스 url은 중복

    def test_수정_후_변경값이_반영된다(self, db_session):
        repo = TechNewsSourceRepository(db_session)
        saved = _source(db_session)

        saved.name = "변경된 이름"
        saved.url = "https://changed.example.com/rss"
        saved.is_active = False
        repo.save(saved)
        db_session.commit()

        found = repo.find_by_id(saved.id)
        assert found.name == "변경된 이름"
        assert found.url == "https://changed.example.com/rss"
        assert found.is_active is False

    def test_삭제하면_조회되지_않는다(self, db_session):
        repo = TechNewsSourceRepository(db_session)
        saved = _source(db_session)
        source_id = saved.id

        repo.delete(saved)
        db_session.commit()

        assert repo.find_by_id(source_id) is None
        assert repo.find_all_order_by_id_asc() == []
