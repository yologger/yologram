import pytest
from sqlalchemy import create_engine
from sqlalchemy.orm import sessionmaker
from testcontainers.mysql import MySqlContainer

from app.config.database import Base
from app.domain.pms.tech.model import TechPostCommentCount
from app.domain.pms.tech.repository import TechPostCommentCountRepository


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
def clean_table(db_session):
    """테스트 간 격리: 카운트 테이블 초기화."""
    db_session.query(TechPostCommentCount).delete()
    db_session.commit()


def _find_count(db_session, post_id: int) -> TechPostCommentCount | None:
    """네이티브 갱신 결과를 1차 캐시(stale 엔티티) 없이 다시 읽는다."""
    db_session.expire_all()
    return (
        db_session.query(TechPostCommentCount)
        .filter(TechPostCommentCount.post_id == post_id)
        .first()
    )


class TestTechPostCommentCountRepository:

    class TestIncrease:

        def test_row가_없으면_comment_count_1로_생성한다_upsert(self, db_session):
            repo = TechPostCommentCountRepository(db_session)

            repo.increase(100)
            db_session.commit()

            assert _find_count(db_session, 100).comment_count == 1

        def test_row가_있으면_comment_count를_1_증가시킨다(self, db_session):
            db_session.add(TechPostCommentCount(post_id=100, comment_count=3))
            db_session.commit()
            repo = TechPostCommentCountRepository(db_session)

            repo.increase(100)
            db_session.commit()

            assert _find_count(db_session, 100).comment_count == 4

        def test_연속_호출_시_호출_횟수만큼_누적된다(self, db_session):
            repo = TechPostCommentCountRepository(db_session)

            repo.increase(100)
            repo.increase(100)
            repo.increase(100)
            db_session.commit()

            assert _find_count(db_session, 100).comment_count == 3

    class TestDecrease:

        def test_row가_있으면_comment_count를_1_감소시킨다(self, db_session):
            db_session.add(TechPostCommentCount(post_id=100, comment_count=2))
            db_session.commit()
            repo = TechPostCommentCountRepository(db_session)

            repo.decrease(100)
            db_session.commit()

            assert _find_count(db_session, 100).comment_count == 1

        def test_0에서_감소해도_음수가_되지_않고_0을_유지한다(self, db_session):
            db_session.add(TechPostCommentCount(post_id=100, comment_count=0))
            db_session.commit()
            repo = TechPostCommentCountRepository(db_session)

            repo.decrease(100)
            db_session.commit()

            assert _find_count(db_session, 100).comment_count == 0

        def test_0이_되어도_row는_삭제하지_않는다(self, db_session):
            repo = TechPostCommentCountRepository(db_session)
            repo.increase(100)
            db_session.commit()

            repo.decrease(100)
            db_session.commit()

            # count 0 유지 + row 존재 (조회 coalesce가 0 처리하므로 삭제/재생성 churn 없음)
            row = _find_count(db_session, 100)
            assert row is not None
            assert row.comment_count == 0

        def test_row가_없으면_아무_일도_없고_row도_생성하지_않는다(self, db_session):
            repo = TechPostCommentCountRepository(db_session)

            repo.decrease(999)
            db_session.commit()

            assert _find_count(db_session, 999) is None
