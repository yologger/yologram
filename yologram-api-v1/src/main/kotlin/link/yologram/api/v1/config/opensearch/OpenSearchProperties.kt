package link.yologram.api.v1.config.opensearch

import org.springframework.boot.context.properties.ConfigurationProperties

/**
 * OpenSearch 접속 설정 — CacheRedisProperties와 같은 패턴(커스텀 프로퍼티 + 수동 빈).
 *
 * 셀프호스팅(Lightsail)이라 IAM이 아니라 security plugin의 basic auth로 인증한다.
 * prod는 네 값 모두 Parameter Store에서 주입(/yologram/service/yologram-worker_prod/opensearch.main.*).
 * main 세그먼트는 database.main.* 규칙과 같은 결 — 클러스터가 늘면 여기서 갈라진다.
 *
 * enabled=false(로컬·테스트 기본)면 클라이언트 빈을 만들지 않는다 —
 * 검색 서비스 빈도 함께 만들어지지 않으므로 로컬이 prod 인덱스를 건드릴 수 없다.
 */
@ConfigurationProperties(prefix = "opensearch.main")
data class OpenSearchProperties(
    val enabled: Boolean = false,
    val uri: String = "",
    val username: String = "",
    val password: String = "",
)
