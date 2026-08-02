from types import SimpleNamespace
from unittest.mock import MagicMock

from app.infra.cache.cache import Cache
from app.infra.cache.user_nickname_cache import UserNicknameCache
from app.infra.client.ums.ums_api_client import LocalUmsApiClient


def _user(uid: int, nickname: str):
    return SimpleNamespace(id=uid, nickname=nickname)


def _client(mock_cache_service: MagicMock) -> LocalUmsApiClient:
    client = LocalUmsApiClient(MagicMock(), nickname_cache=UserNicknameCache(mock_cache_service))
    client.repository = MagicMock()
    return client


class TestLocalUmsApiClient:

    class TestFindNickname:

        def test_캐시_히트면_DB를_조회하지_않는다(self):
            cache_service = MagicMock()
            cache_service.get_or_null.return_value = "캐시닉"
            client = _client(cache_service)

            result = client.find_nickname(1)

            assert result == "캐시닉"
            client.repository.find_by_id.assert_not_called()
            cache_service.set.assert_not_called()

        def test_미스면_DB에서_읽고_캐시에_채운다(self):
            cache_service = MagicMock()
            cache_service.get_or_null.return_value = None
            client = _client(cache_service)
            client.repository.find_by_id.return_value = _user(1, "디비닉")

            result = client.find_nickname(1)

            assert result == "디비닉"
            client.repository.find_by_id.assert_called_once_with(1)
            cache_service.set.assert_called_once_with(Cache.user_nickname(1), "디비닉")

        def test_유저가_없으면_None을_반환하고_캐시하지_않는다(self):
            cache_service = MagicMock()
            cache_service.get_or_null.return_value = None
            client = _client(cache_service)
            client.repository.find_by_id.return_value = None

            result = client.find_nickname(404)

            assert result is None
            cache_service.set.assert_not_called()

    class TestFindNicknames:

        def test_전체_히트면_DB를_조회하지_않는다(self):
            cache_service = MagicMock()
            cache_service.get_all_as_map.return_value = {
                Cache.user_nickname(1).key: "닉1",
                Cache.user_nickname(2).key: "닉2",
            }
            client = _client(cache_service)

            result = client.find_nicknames([1, 2])

            assert result == {1: "닉1", 2: "닉2"}
            client.repository.find_by_ids.assert_not_called()
            cache_service.set_all.assert_not_called()

        def test_부분_미스면_미스_uid만_DB_조회하고_캐시에_채운다(self):
            cache_service = MagicMock()
            cache_service.get_all_as_map.return_value = {Cache.user_nickname(1).key: "닉1"}
            client = _client(cache_service)
            client.repository.find_by_ids.return_value = [_user(2, "닉2")]

            result = client.find_nicknames([1, 2])

            assert result == {1: "닉1", 2: "닉2"}
            (called_uids,), _ = client.repository.find_by_ids.call_args
            assert list(called_uids) == [2]  # 미스분만 IN 조회
            cache_service.set_all.assert_called_once_with({Cache.user_nickname(2): "닉2"})

        def test_장애로_전체_미스면_전부_DB_폴백된다(self):
            cache_service = MagicMock()
            cache_service.get_all_as_map.return_value = {}  # Redis 장애 시 빈 맵
            client = _client(cache_service)
            client.repository.find_by_ids.return_value = [_user(1, "닉1"), _user(2, "닉2")]

            result = client.find_nicknames([1, 2])

            assert result == {1: "닉1", 2: "닉2"}
            (called_uids,), _ = client.repository.find_by_ids.call_args
            assert sorted(called_uids) == [1, 2]

        def test_DB에도_없는_uid는_결과에서_제외되고_캐시되지_않는다(self):
            cache_service = MagicMock()
            cache_service.get_all_as_map.return_value = {}
            client = _client(cache_service)
            client.repository.find_by_ids.return_value = [_user(1, "닉1")]  # 2는 탈퇴 등으로 없음

            result = client.find_nicknames([1, 2])

            assert result == {1: "닉1"}
            cache_service.set_all.assert_called_once_with({Cache.user_nickname(1): "닉1"})

        def test_빈_입력이면_캐시도_DB도_호출하지_않는다(self):
            cache_service = MagicMock()
            client = _client(cache_service)

            assert client.find_nicknames([]) == {}
            cache_service.get_all_as_map.assert_not_called()
            client.repository.find_by_ids.assert_not_called()

        def test_중복_uid는_한_번만_조회된다(self):
            cache_service = MagicMock()
            cache_service.get_all_as_map.return_value = {}
            client = _client(cache_service)
            client.repository.find_by_ids.return_value = [_user(1, "닉1")]

            result = client.find_nicknames([1, 1, 1])

            assert result == {1: "닉1"}
            (called_uids,), _ = client.repository.find_by_ids.call_args
            assert list(called_uids) == [1]
