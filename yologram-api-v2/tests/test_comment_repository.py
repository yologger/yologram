import pytest
from sqlalchemy import create_engine
from sqlalchemy.orm import sessionmaker
from testcontainers.mysql import MySqlContainer

from app.config.database import Base
from app.domain.comment.model import Comment
from app.domain.comment.repository import CommentRepository


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


class TestCommentRepository:

    class TestDeleteByPostId:

        def test_해당_글의_댓글만_전체_삭제되고_다른_글의_댓글은_보존(self, db_session):
            repo = CommentRepository(db_session)
            repo.save(Comment(post_id=1, user_id=1, content="글1 댓글1"))
            repo.save(Comment(post_id=1, user_id=2, content="글1 댓글2"))
            other = repo.save(Comment(post_id=2, user_id=1, content="글2 댓글"))
            db_session.commit()

            repo.delete_by_post_id(1)
            db_session.commit()

            assert repo.count_by_post(1) == 0
            assert repo.count_by_post(2) == 1
            assert repo.find_by_id(other.id) is not None

        def test_댓글이_없는_글이면_아무것도_삭제하지_않고_에러_없음(self, db_session):
            repo = CommentRepository(db_session)
            before = repo.count_by_post(2)

            repo.delete_by_post_id(999)
            db_session.commit()

            assert repo.count_by_post(999) == 0
            assert repo.count_by_post(2) == before
