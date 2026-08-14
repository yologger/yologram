from functools import lru_cache

from opensearchpy import OpenSearch

from app.config.settings import get_settings


@lru_cache
def get_opensearch_client() -> OpenSearch:
    """
    검색용 OpenSearch 클라이언트 (api-v1 OpenSearchConfig 미러) —
    lru_cache로 앱 수명주기 동안 재사용(요청마다 만들면 커넥션 풀이 매번 새로 생긴다).

    셀프호스팅이라 IAM이 아니라 security plugin의 basic auth로 인증한다.
    Caddy가 정식 인증서로 TLS를 종료하므로 검증을 끄지 않는다 —
    self-signed 예외는 컨테이너 내부(Dashboards→OpenSearch) 구간에만 있다.

    타임아웃은 검색이 사용자 응답 경로라 짧게 둔다(연결·읽기 3초, 재시도 1회).
    호출부(TechPostSearchRepository)는 enabled일 때만 이 함수를 부르므로
    설정이 없는 환경(테스트·CI)에서는 클라이언트가 만들어지지 않는다.
    """
    settings = get_settings()
    return OpenSearch(
        hosts=[settings.opensearch_main_uri],
        http_auth=(settings.opensearch_main_username, settings.opensearch_main_password),
        use_ssl=settings.opensearch_main_uri.startswith("https"),
        verify_certs=True,
        timeout=3,
        max_retries=1,
        retry_on_timeout=True,
    )
