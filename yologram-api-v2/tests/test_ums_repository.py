import pytest
from sqlalchemy import create_engine
from sqlalchemy.orm import sessionmaker
from testcontainers.mysql import MySqlContainer

from app.config.database import Base
from app.domain.ums.enum import UserStatus
from app.domain.ums.model import AdminUser, User
from app.domain.ums.repository import AdminUserRepository, UserRepository


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


class TestAdminUserRepository:

    class TestFindPageOrderByIdAsc:

        def test_offset_limit으로_id_오름차순_페이지를_반환한다(self, db_session):
            db_session.query(AdminUser).delete()
            repo = AdminUserRepository(db_session)
            first = repo.save(AdminUser(email="a1@yologram.link", name="어드민1", password="hashed"))
            second = repo.save(AdminUser(email="a2@yologram.link", name="어드민2", password="hashed"))
            third = repo.save(AdminUser(email="a3@yologram.link", name="어드민3", password="hashed"))
            db_session.commit()
            ids = sorted([first.id, second.id, third.id])

            page1 = repo.find_page_order_by_id_asc(0, 2)
            page2 = repo.find_page_order_by_id_asc(2, 2)

            assert [a.id for a in page1] == ids[:2]  # 첫 페이지 id asc
            assert [a.id for a in page2] == ids[2:]  # 둘째 페이지 잔여분
            assert page1[0].status == UserStatus.ACTIVE  # 기본 상태
            assert page1[0].joined_date is not None

        def test_범위_밖_offset이면_빈_목록을_반환한다(self, db_session):
            db_session.query(AdminUser).delete()
            repo = AdminUserRepository(db_session)
            repo.save(AdminUser(email="a1@yologram.link", name="어드민1", password="hashed"))
            db_session.commit()

            assert repo.find_page_order_by_id_asc(10, 2) == []

    class TestCount:

        def test_전체_어드민_수를_반환한다(self, db_session):
            db_session.query(AdminUser).delete()
            repo = AdminUserRepository(db_session)
            repo.save(AdminUser(email="c1@yologram.link", name="어드민1", password="hashed"))
            repo.save(AdminUser(email="c2@yologram.link", name="어드민2", password="hashed"))
            db_session.commit()

            assert repo.count() == 2

        def test_어드민이_없으면_0을_반환한다(self, db_session):
            db_session.query(AdminUser).delete()
            db_session.commit()
            repo = AdminUserRepository(db_session)

            assert repo.count() == 0

    class TestDelete:

        def test_삭제하면_조회되지_않는다(self, db_session):
            db_session.query(AdminUser).delete()
            repo = AdminUserRepository(db_session)
            saved = repo.save(AdminUser(email="del@yologram.link", name="삭제대상", password="hashed"))
            db_session.commit()
            admin_id = saved.id

            repo.delete(saved)
            db_session.commit()

            assert repo.find_by_id(admin_id) is None
            assert repo.count() == 0
