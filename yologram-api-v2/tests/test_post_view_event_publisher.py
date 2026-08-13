import json
import re
from datetime import datetime
from unittest.mock import MagicMock, patch

from botocore.exceptions import ClientError, EndpointConnectionError

from app.domain.pms.tech.event import PostViewEvent
from app.infra.event.post_view_event_publisher import KinesisPostViewEventPublisher

STREAM_NAME = "yologram-post-view-event-test"

# 초 단위 ISO (마이크로초·타임존 오프셋 없음) — api-v1 LocalDateTime 직렬화와 동일 포맷
ISO_SECONDS_PATTERN = re.compile(r"^\d{4}-\d{2}-\d{2}T\d{2}:\d{2}:\d{2}$")


def _publish(event: PostViewEvent, stream_name: str | None = STREAM_NAME) -> MagicMock:
    """publisher로 발행하고 mock Kinesis 클라이언트를 반환 (put_record 인자 검증용)."""
    with patch("app.infra.event.post_view_event_publisher.get_kinesis_client") as mock_get_client:
        mock_client = MagicMock()
        mock_get_client.return_value = mock_client
        KinesisPostViewEventPublisher(stream_name=stream_name).publish(event)
        return mock_client


def _captured_payload(mock_client: MagicMock) -> dict:
    return json.loads(mock_client.put_record.call_args.kwargs["Data"].decode("utf-8"))


class TestKinesisPostViewEventPublisher:

    def test_페이로드는_필드_6개와_고정값을_가진다(self):
        event = PostViewEvent(
            post_id=1200, uid=12, ip="1.2.3.4", occurred_at=datetime(2026, 8, 12, 21, 30, 0)
        )

        mock_client = _publish(event)

        payload = _captured_payload(mock_client)
        # 필드명·개수·순서까지 api-v1 계약과 동일해야 한다 (같은 스트림을 worker 하나가 소비)
        assert list(payload.keys()) == ["eventType", "section", "postId", "uid", "ip", "occurredAt"]
        assert payload == {
            "eventType": "POST_VIEW",
            "section": "TECH",
            "postId": 1200,
            "uid": 12,
            "ip": "1.2.3.4",
            "occurredAt": "2026-08-12T21:30:00",
        }

    def test_partitionKey는_postId_문자열이다(self):
        mock_client = _publish(PostViewEvent(post_id=1200, uid=None, ip=None))

        kwargs = mock_client.put_record.call_args.kwargs
        assert kwargs["StreamName"] == STREAM_NAME
        assert kwargs["PartitionKey"] == "1200"

    def test_occurredAt은_초_단위_ISO_포맷이다(self):
        # 기본값(현재 시각)도 마이크로초 없이 직렬화되어야 한다
        mock_client = _publish(PostViewEvent(post_id=1, uid=None, ip=None))

        occurred_at = _captured_payload(mock_client)["occurredAt"]
        assert ISO_SECONDS_PATTERN.match(occurred_at), occurred_at

    def test_마이크로초가_있는_시각도_초_단위로_잘린다(self):
        event = PostViewEvent(post_id=1, occurred_at=datetime(2026, 8, 12, 21, 30, 0, 123456))

        payload = _captured_payload(_publish(event))

        assert payload["occurredAt"] == "2026-08-12T21:30:00"

    def test_로그인_유저면_uid를_담는다(self):
        payload = _captured_payload(_publish(PostViewEvent(post_id=5, uid=42, ip="1.1.1.1")))

        assert payload["uid"] == 42

    def test_비로그인이면_uid는_null이다(self):
        payload = _captured_payload(_publish(PostViewEvent(post_id=5, uid=None, ip="1.1.1.1")))

        assert payload["uid"] is None

    def test_ip를_모르면_null이다(self):
        payload = _captured_payload(_publish(PostViewEvent(post_id=5, uid=1, ip=None)))

        assert payload["ip"] is None

    def test_스트림_이름이_빈_값이면_발행하지_않는다(self):
        mock_client = _publish(PostViewEvent(post_id=1), stream_name="")

        mock_client.put_record.assert_not_called()

    def test_스트림_이름이_공백만이면_발행하지_않는다(self):
        mock_client = _publish(PostViewEvent(post_id=1), stream_name="   ")

        mock_client.put_record.assert_not_called()

    def test_스트림_이름_미설정이면_클라이언트를_만들지도_않는다(self):
        # 로컬·테스트 기본(설정값 빈 문자열) — 자격증명 없는 환경에서도 안전
        with patch("app.infra.event.post_view_event_publisher.get_settings") as mock_get_settings:
            mock_get_settings.return_value = MagicMock(post_view_stream_name="")
            with patch("app.infra.event.post_view_event_publisher.get_kinesis_client") as mock_get_client:
                KinesisPostViewEventPublisher().publish(PostViewEvent(post_id=1))

                mock_get_client.assert_not_called()

    def test_설정값이_있으면_스트림_이름으로_사용한다(self):
        with patch("app.infra.event.post_view_event_publisher.get_settings") as mock_get_settings:
            mock_get_settings.return_value = MagicMock(post_view_stream_name=STREAM_NAME)

            assert KinesisPostViewEventPublisher().stream_name == STREAM_NAME

    def test_발행_실패는_삼킨다(self):
        # 부가 기능이므로 예외를 전파하지 않는다 (조회 API 가용성 우선)
        with patch("app.infra.event.post_view_event_publisher.get_kinesis_client") as mock_get_client:
            mock_client = MagicMock()
            mock_client.put_record.side_effect = ClientError(
                {"Error": {"Code": "ProvisionedThroughputExceededException", "Message": "throttled"}},
                "PutRecord",
            )
            mock_get_client.return_value = mock_client

            KinesisPostViewEventPublisher(stream_name=STREAM_NAME).publish(PostViewEvent(post_id=1))

    def test_네트워크_예외도_삼킨다(self):
        with patch("app.infra.event.post_view_event_publisher.get_kinesis_client") as mock_get_client:
            mock_client = MagicMock()
            mock_client.put_record.side_effect = EndpointConnectionError(endpoint_url="https://kinesis")
            mock_get_client.return_value = mock_client

            KinesisPostViewEventPublisher(stream_name=STREAM_NAME).publish(PostViewEvent(post_id=1))

    def test_클라이언트_생성_실패도_삼킨다(self):
        # 자격증명 없음 등으로 클라이언트 생성 자체가 실패해도 조회는 정상이어야 한다
        with patch("app.infra.event.post_view_event_publisher.get_kinesis_client") as mock_get_client:
            mock_get_client.side_effect = Exception("no credentials")

            KinesisPostViewEventPublisher(stream_name=STREAM_NAME).publish(PostViewEvent(post_id=1))

    def test_한글_ip_헤더값도_그대로_직렬화된다(self):
        # ensure_ascii=False — 비ASCII도 이스케이프 없이 (api-v1 Jackson 표현과 바이트 호환)
        payload = _captured_payload(_publish(PostViewEvent(post_id=1, ip="한글")))

        assert payload["ip"] == "한글"
