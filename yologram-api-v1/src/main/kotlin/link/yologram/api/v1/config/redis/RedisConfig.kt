package link.yologram.api.v1.config.redis

import io.lettuce.core.ClientOptions
import io.lettuce.core.SocketOptions
import io.lettuce.core.TimeoutOptions
import io.lettuce.core.resource.ClientResources
import io.lettuce.core.resource.DefaultClientResources
import io.lettuce.core.resource.Delay
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.data.redis.connection.RedisStandaloneConfiguration
import org.springframework.data.redis.connection.lettuce.LettuceClientConfiguration
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory
import org.springframework.data.redis.core.StringRedisTemplate
import java.time.Duration
import java.util.concurrent.TimeUnit

/**
 * Redis(Valkey) 수동 빈 구성 — DataSource(CoreDatabaseConfig)와 동일 패턴:
 * Redis 자동구성 exclude + 커스텀 프로퍼티(cache.data.redis.*) + 수동 빈.
 * 연결은 lazy — 첫 캐시 연산 시점에 커넥션을 맺으므로 Redis 미기동 환경(테스트 등)에서도 부팅에 영향 없음.
 *
 * [AWS Guide](https://docs.aws.amazon.com/AmazonElastiCache/latest/red-ug/BestPractices.Clients-lettuce.html)
 */
@Configuration
class RedisConfig(private val properties: CacheRedisProperties) {

    /** reconnect fullJitter(100ms~3s) — 레거시 RedisConfig의 ClientResourcesBuilderCustomizer 이관 */
    @Bean(destroyMethod = "shutdown")
    fun clientResources(): ClientResources {
        return DefaultClientResources.builder()
            .reconnectDelay {
                Delay.fullJitter(
                    Duration.ofMillis(100),     // minimum 100 millisecond delay
                    Duration.ofSeconds(3),      // maximum 3 second delay
                    100,                        // 100 millisecond base
                    TimeUnit.MILLISECONDS,
                )
            }
            .build()
    }

    /**
     * 클러스터 전환 시 확장 지점 — RedisStandaloneConfiguration을 RedisClusterConfiguration으로 바꾸고
     * ClusterClientOptions(토폴로지 리프레시: periodic 30s + adaptive 트리거, validateClusterNodeMembership false,
     * FAIL 계열 노드 필터) + ReadFrom.REPLICA_PREFERRED를 적용한다.
     * 레거시 참고: yologram-legacy/api/yologram-api/src/main/kotlin/link/yologram/api/config/RedisConfig.kt
     */
    @Bean
    fun redisConnectionFactory(clientResources: ClientResources): LettuceConnectionFactory {
        // 캐시는 부가 기능 — Redis 장애가 API 지연으로 전파되지 않도록 짧은 타임아웃 + 끊김 시 즉시 거부.
        // (기본값이면 command timeout 60초 × 요청당 캐시 연산 수만큼 지연이 누적됨 — 실측 120초 사례)
        val clientOptions = ClientOptions.builder()
            .socketOptions(
                SocketOptions.builder()
                    .keepAlive(true)
                    .connectTimeout(Duration.ofSeconds(1))
                    .build()
            )
            .timeoutOptions(TimeoutOptions.enabled(Duration.ofSeconds(1)))
            // 연결 끊김 상태에서는 명령을 버퍼링(재연결 대기)하지 않고 즉시 실패 → runCatching 폴백이 바로 동작
            .disconnectedBehavior(ClientOptions.DisconnectedBehavior.REJECT_COMMANDS)
            .build()

        val clientConfiguration = LettuceClientConfiguration.builder()
            .clientResources(clientResources)
            .clientOptions(clientOptions)
            .commandTimeout(Duration.ofSeconds(1))
            .build()

        return LettuceConnectionFactory(
            RedisStandaloneConfiguration(properties.host, properties.port),
            clientConfiguration,
        )
    }

    @Bean
    fun stringRedisTemplate(redisConnectionFactory: LettuceConnectionFactory): StringRedisTemplate {
        return StringRedisTemplate(redisConnectionFactory)
    }
}
