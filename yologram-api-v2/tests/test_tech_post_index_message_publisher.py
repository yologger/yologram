import json
from unittest.mock import MagicMock, patch

import pytest
from botocore.exceptions import ClientError

from app.domain.search.tech.publisher.message.tech_post_index_message import (
    TARGET_TECH_POST,
    TechPostIndexMessage,
)
from app.domain.search.tech.publisher.message.tech_post_index_message_publisher import (
    SqsTechPostIndexMessagePublisher,
)

QUEUE_NAME = "yologram-search-indexing-test"
QUEUE_URL = "https://sqs.ap-northeast-2.amazonaws.com/123456789012/yologram-search-indexing-test"

PATCH_TARGET = "app.domain.search.tech.publisher.message.tech_post_index_message_publisher.get_sqs_client"


def _mock_client() -> MagicMock:
    client = MagicMock()
    client.get_queue_url.return_value = {"QueueUrl": QUEUE_URL}
    return client


def _captured_body(client: MagicMock) -> dict:
    return json.loads(client.send_message.call_args.kwargs["MessageBody"])


class TestPublish:

    def test_조회한_큐_URL로_메시지를_보낸다(self):
        with patch(PATCH_TARGET) as mock_get_client:
            client = _mock_client()
            mock_get_client.return_value = client

            SqsTechPostIndexMessagePublisher(queue_name=QUEUE_NAME, enabled=True).publish(
                TechPostIndexMessage(from_id=1, to_id=20)
            )

            assert client.send_message.call_args.kwargs["QueueUrl"] == QUEUE_URL

    def test_본문은_target과_범위를_담은_JSON이다(self):
        with patch(PATCH_TARGET) as mock_get_client:
            client = _mock_client()
            mock_get_client.return_value = client

            SqsTechPostIndexMessagePublisher(queue_name=QUEUE_NAME, enabled=True).publish(
                TechPostIndexMessage(from_id=1, to_id=20)
            )

            # worker가 이 세 필드로 역직렬화한다 — 이름이 바뀌면 소비가 깨진다
            assert _captured_body(client) == {"target": TARGET_TECH_POST, "from": 1, "to": 20}

    def test_큐_URL은_한_번만_조회하고_재사용한다(self):
        with patch(PATCH_TARGET) as mock_get_client:
            client = _mock_client()
            mock_get_client.return_value = client
            publisher = SqsTechPostIndexMessagePublisher(queue_name=QUEUE_NAME, enabled=True)

            publisher.publish(TechPostIndexMessage(from_id=1, to_id=20))
            publisher.publish(TechPostIndexMessage(from_id=21, to_id=40))

            assert client.get_queue_url.call_count == 1
            assert client.send_message.call_count == 2

    def test_api_v1과_동일한_직렬화_포맷이다(self):
        with patch(PATCH_TARGET) as mock_get_client:
            client = _mock_client()
            mock_get_client.return_value = client

            SqsTechPostIndexMessagePublisher(queue_name=QUEUE_NAME, enabled=True).publish(
                TechPostIndexMessage(from_id=1, to_id=1)
            )

            # 구분자 공백 없음 — api-v1(Jackson) 출력과 바이트 수준 동일
            assert client.send_message.call_args.kwargs["MessageBody"] == '{"target":"TECH_POST","from":1,"to":1}'


class TestSkip:

    def test_비활성이면_SQS를_호출하지_않는다(self):
        with patch(PATCH_TARGET) as mock_get_client:
            SqsTechPostIndexMessagePublisher(queue_name=QUEUE_NAME, enabled=False).publish(
                TechPostIndexMessage(from_id=1, to_id=20)
            )

            # 클라이언트를 아예 만들지 않는다 (자격증명 없는 환경 보호)
            mock_get_client.assert_not_called()

    def test_활성인데_큐_이름이_없으면_보내지_않는다(self):
        with patch(PATCH_TARGET) as mock_get_client:
            SqsTechPostIndexMessagePublisher(queue_name=None, enabled=True).publish(
                TechPostIndexMessage(from_id=1, to_id=20)
            )

            mock_get_client.assert_not_called()

    def test_큐_이름이_공백이면_보내지_않는다(self):
        with patch(PATCH_TARGET) as mock_get_client:
            SqsTechPostIndexMessagePublisher(queue_name="   ", enabled=True).publish(
                TechPostIndexMessage(from_id=1, to_id=20)
            )

            mock_get_client.assert_not_called()


class TestIsEnabled:

    def test_활성이고_큐가_있으면_True(self):
        assert SqsTechPostIndexMessagePublisher(queue_name=QUEUE_NAME, enabled=True).is_enabled()

    def test_비활성이면_False(self):
        assert not SqsTechPostIndexMessagePublisher(queue_name=QUEUE_NAME, enabled=False).is_enabled()

    def test_큐가_없으면_False(self):
        assert not SqsTechPostIndexMessagePublisher(queue_name=None, enabled=True).is_enabled()


class TestFailure:

    def test_전송_실패는_삼키지_않고_전파한다(self):
        # 조회 이벤트 발행과 다르다 — 어드민이 명시 요청한 작업이라 실패를 알려야 한다
        with patch(PATCH_TARGET) as mock_get_client:
            client = _mock_client()
            client.send_message.side_effect = ClientError(
                {"Error": {"Code": "AWS.SimpleQueueService.NonExistentQueue"}}, "SendMessage"
            )
            mock_get_client.return_value = client

            with pytest.raises(ClientError):
                SqsTechPostIndexMessagePublisher(queue_name=QUEUE_NAME, enabled=True).publish(
                    TechPostIndexMessage(from_id=1, to_id=20)
                )

    def test_큐_URL_조회_실패도_전파한다(self):
        with patch(PATCH_TARGET) as mock_get_client:
            client = MagicMock()
            client.get_queue_url.side_effect = ClientError(
                {"Error": {"Code": "AWS.SimpleQueueService.NonExistentQueue"}}, "GetQueueUrl"
            )
            mock_get_client.return_value = client

            with pytest.raises(ClientError):
                SqsTechPostIndexMessagePublisher(queue_name=QUEUE_NAME, enabled=True).publish(
                    TechPostIndexMessage(from_id=1, to_id=20)
                )

            client.send_message.assert_not_called()
