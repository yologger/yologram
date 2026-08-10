import pytest
from sqlalchemy import create_engine
from sqlalchemy.orm import sessionmaker
from testcontainers.mysql import MySqlContainer

from app.config.database import Base
from app.domain.pms.tech.model import TechPost, TechPostCommentCount
from app.domain.pms.tech.repository import TechPostRepository


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
    """테스트 간 격리: 게시글·카운트 테이블 초기화."""
    db_session.query(TechPost).delete()
    db_session.query(TechPostCommentCount).delete()
    db_session.commit()


def _save_post(db_session, user_id: int = 1, content: str = "내용") -> TechPost:
    post = TechPost(user_id=user_id, content=content)
    db_session.add(post)
    db_session.commit()
    return post


class TestTechPostRepositoryCommentCount:
    """댓글 수 outerjoin + coalesce(0) 프로젝션 — count row가 있는 글은 실값, 없는 글은 0."""

    class TestFindPostWithCommentCount:

        def test_카운트_row가_있는_글은_실값을_반환(self, db_session):
            post = _save_post(db_session)
            db_session.add(TechPostCommentCount(post_id=post.id, comment_count=3))
            db_session.commit()
            repo = TechPostRepository(db_session)

            result = repo.find_post_with_comment_count(post.id)

            assert result.post.id == post.id
            assert result.comment_count == 3

        def test_카운트_row가_없는_글은_0을_반환(self, db_session):
            post = _save_post(db_session)
            repo = TechPostRepository(db_session)

            result = repo.find_post_with_comment_count(post.id)

            assert result.post.id == post.id
            assert result.comment_count == 0

        def test_없는_글이면_None(self, db_session):
            repo = TechPostRepository(db_session)

            assert repo.find_post_with_comment_count(999) is None

    class TestFindPosts:

        def test_카운트_row_유무와_무관하게_목록에서_빠지지_않고_실값과_0으로_나온다(self, db_session):
            with_count = _save_post(db_session, content="카운트 있는 글")
            without_count = _save_post(db_session, content="카운트 없는 글")
            db_session.add(TechPostCommentCount(post_id=with_count.id, comment_count=2))
            db_session.commit()
            repo = TechPostRepository(db_session)

            results = repo.find_posts(None, None, 10)

            # id desc — 나중 글(카운트 없음)이 먼저, 1:1 join이라 row 불어남 없음
            assert [r.post.id for r in results] == [without_count.id, with_count.id]
            assert results[0].comment_count == 0
            assert results[1].comment_count == 2

    class TestFindMyPostsByCursor:

        def test_내_글_목록도_댓글_수_실값과_0으로_나온다(self, db_session):
            mine = _save_post(db_session, user_id=1, content="내 글")
            _save_post(db_session, user_id=2, content="남의 글")
            db_session.add(TechPostCommentCount(post_id=mine.id, comment_count=5))
            db_session.commit()
            repo = TechPostRepository(db_session)

            results = repo.find_my_posts_by_cursor(1, None, 10)

            assert [r.post.id for r in results] == [mine.id]
            assert results[0].comment_count == 5
