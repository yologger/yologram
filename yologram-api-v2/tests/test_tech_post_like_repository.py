import pytest
from sqlalchemy import create_engine
from sqlalchemy.orm import sessionmaker
from testcontainers.mysql import MySqlContainer

from app.config.database import Base
from app.domain.pms.tech.model import TechPostLike, TechPostLikeCount
from app.domain.pms.tech.repository import TechPostLikeCountRepository, TechPostLikeRepository


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
def clean_tables(db_session):
    """테스트 간 격리: 이력·카운트 테이블 초기화."""
    db_session.query(TechPostLike).delete()
    db_session.query(TechPostLikeCount).delete()
    db_session.commit()


class TestTechPostLikeRepository:

    class TestInsertIgnore:

        def test_처음_삽입하면_1을_반환하고_이력_row가_생긴다(self, db_session):
            repo = TechPostLikeRepository(db_session)

            inserted = repo.insert_ignore(100, 7)
            db_session.commit()

            assert inserted == 1
            assert repo.exists_by_post_id_and_uid(100, 7) is True

        def test_이미_있으면_0을_반환하고_row가_늘지_않는다_멱등(self, db_session):
            repo = TechPostLikeRepository(db_session)
            repo.insert_ignore(100, 7)
            db_session.commit()

            inserted = repo.insert_ignore(100, 7)
            db_session.commit()

            # INSERT IGNORE — uk(post_id, uid) 충돌을 에러 없이 0행 삽입으로 무시
            assert inserted == 0
            assert db_session.query(TechPostLike).filter_by(post_id=100, uid=7).count() == 1

        def test_다른_유저_다른_글은_각각_삽입된다(self, db_session):
            repo = TechPostLikeRepository(db_session)

            assert repo.insert_ignore(100, 7) == 1
            assert repo.insert_ignore(100, 8) == 1
            assert repo.insert_ignore(200, 7) == 1
            db_session.commit()

            assert db_session.query(TechPostLike).count() == 3

    class TestDeleteByPostIdAndUid:

        def test_있으면_1을_반환하고_삭제된다(self, db_session):
            repo = TechPostLikeRepository(db_session)
            repo.insert_ignore(100, 7)
            db_session.commit()

            deleted = repo.delete_by_post_id_and_uid(100, 7)
            db_session.commit()

            assert deleted == 1
            assert repo.exists_by_post_id_and_uid(100, 7) is False

        def test_없으면_0을_반환한다_멱등(self, db_session):
            repo = TechPostLikeRepository(db_session)

            assert repo.delete_by_post_id_and_uid(100, 7) == 0

        def test_다른_유저의_좋아요는_지우지_않는다(self, db_session):
            repo = TechPostLikeRepository(db_session)
            repo.insert_ignore(100, 7)
            repo.insert_ignore(100, 8)
            db_session.commit()

            repo.delete_by_post_id_and_uid(100, 7)
            db_session.commit()

            assert repo.exists_by_post_id_and_uid(100, 8) is True

    class TestFindLikedPostIds:

        def test_유저가_누른_글의_post_id_Set을_반환(self, db_session):
            repo = TechPostLikeRepository(db_session)
            repo.insert_ignore(100, 7)
            repo.insert_ignore(300, 7)
            repo.insert_ignore(200, 8)  # 다른 유저
            db_session.commit()

            result = repo.find_liked_post_ids(7, [100, 200, 300])

            assert result == {100, 300}

        def test_빈_post_ids면_조회_없이_빈_Set(self, db_session):
            repo = TechPostLikeRepository(db_session)

            assert repo.find_liked_post_ids(7, []) == set()

        def test_누른_글이_없으면_빈_Set(self, db_session):
            repo = TechPostLikeRepository(db_session)

            assert repo.find_liked_post_ids(7, [100, 200]) == set()


class TestTechPostLikeCountRepository:

    def _find_count(self, db_session, post_id: int) -> TechPostLikeCount | None:
        db_session.expire_all()
        return db_session.query(TechPostLikeCount).filter_by(post_id=post_id).first()

    class TestIncrease:

        def test_row가_없으면_like_count_1로_생성한다_upsert(self, db_session):
            repo = TechPostLikeCountRepository(db_session)

            repo.increase(100)
            db_session.commit()

            db_session.expire_all()
            assert db_session.query(TechPostLikeCount).filter_by(post_id=100).first().like_count == 1

        def test_row가_있으면_like_count를_1_증가시킨다(self, db_session):
            db_session.add(TechPostLikeCount(post_id=100, like_count=3))
            db_session.commit()
            repo = TechPostLikeCountRepository(db_session)

            repo.increase(100)
            db_session.commit()

            db_session.expire_all()
            assert db_session.query(TechPostLikeCount).filter_by(post_id=100).first().like_count == 4

        def test_연속_호출_시_호출_횟수만큼_누적된다(self, db_session):
            repo = TechPostLikeCountRepository(db_session)

            repo.increase(100)
            repo.increase(100)
            repo.increase(100)
            db_session.commit()

            db_session.expire_all()
            assert db_session.query(TechPostLikeCount).filter_by(post_id=100).first().like_count == 3

    class TestDecrease:

        def test_row가_있으면_like_count를_1_감소시킨다(self, db_session):
            db_session.add(TechPostLikeCount(post_id=100, like_count=2))
            db_session.commit()
            repo = TechPostLikeCountRepository(db_session)

            repo.decrease(100)
            db_session.commit()

            db_session.expire_all()
            assert db_session.query(TechPostLikeCount).filter_by(post_id=100).first().like_count == 1

        def test_0에서_감소해도_음수가_되지_않고_0을_유지한다(self, db_session):
            db_session.add(TechPostLikeCount(post_id=100, like_count=0))
            db_session.commit()
            repo = TechPostLikeCountRepository(db_session)

            repo.decrease(100)
            db_session.commit()

            db_session.expire_all()
            assert db_session.query(TechPostLikeCount).filter_by(post_id=100).first().like_count == 0

        def test_0이_되어도_row는_삭제하지_않는다(self, db_session):
            repo = TechPostLikeCountRepository(db_session)
            repo.increase(100)
            db_session.commit()

            repo.decrease(100)
            db_session.commit()

            # count 0 유지 + row 존재 (조회 coalesce가 0 처리하므로 삭제/재생성 churn 없음)
            db_session.expire_all()
            row = db_session.query(TechPostLikeCount).filter_by(post_id=100).first()
            assert row is not None
            assert row.like_count == 0

        def test_row가_없으면_아무_일도_없고_row도_생성하지_않는다(self, db_session):
            repo = TechPostLikeCountRepository(db_session)

            repo.decrease(999)
            db_session.commit()

            db_session.expire_all()
            assert db_session.query(TechPostLikeCount).filter_by(post_id=999).first() is None
