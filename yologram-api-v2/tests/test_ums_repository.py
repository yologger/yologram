import pytest
from sqlalchemy import create_engine
from sqlalchemy.orm import sessionmaker
from testcontainers.mysql import MySqlContainer

from app.config.database import Base
from app.domain.ums.model import User
from app.domain.ums.repository import UserRepository


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


class TestUserRepository:

    class TestSave:

        def test_유저_저장_성공(self, db_session):
            repo = UserRepository(db_session)
            user = User(
                email="save@yologram.link",
                name="테스트",
                nickname="tester",
                password="hashed_password",
            )

            saved = repo.save(user)
            db_session.commit()

            assert saved.id is not None
            assert saved.email == "save@yologram.link"

    class TestFindByEmail:

        def test_이메일로_조회_성공(self, db_session):
            repo = UserRepository(db_session)
            user = User(
                email="find@yologram.link",
                name="테스트",
                nickname="finder",
                password="hashed_password",
            )
            repo.save(user)
            db_session.commit()

            found = repo.find_by_email("find@yologram.link")

            assert found is not None
            assert found.email == "find@yologram.link"
            assert found.nickname == "finder"

        def test_존재하지_않는_이메일_조회시_None(self, db_session):
            repo = UserRepository(db_session)

            found = repo.find_by_email("notexist@yologram.link")

            assert found is None
