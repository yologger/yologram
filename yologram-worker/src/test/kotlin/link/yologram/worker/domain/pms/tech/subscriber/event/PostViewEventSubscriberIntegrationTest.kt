package link.yologram.worker.domain.pms.tech.subscriber.event

import link.yologram.worker.domain.pms.tech.repository.TechPostViewCountRepository
import link.yologram.worker.domain.pms.tech.repository.TechPostViewRepository
import org.awaitility.Awaitility.await
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Bean
import org.springframework.data.repository.findByIdOrNull
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import org.testcontainers.containers.localstack.LocalStackContainer
import org.testcontainers.junit.jupiter.Container
import org.testcontainers.junit.jupiter.Testcontainers
import org.testcontainers.utility.DockerImageName
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider
import software.amazon.awssdk.core.SdkBytes
import software.amazon.awssdk.core.interceptor.Context
import software.amazon.awssdk.core.interceptor.ExecutionAttributes
import software.amazon.awssdk.core.interceptor.ExecutionInterceptor
import software.amazon.awssdk.core.interceptor.SdkExecutionAttribute
import software.amazon.awssdk.regions.Region
import software.amazon.awssdk.services.cloudwatch.CloudWatchAsyncClient
import software.amazon.awssdk.services.dynamodb.DynamoDbAsyncClient
import software.amazon.awssdk.services.kinesis.KinesisAsyncClient
import software.amazon.awssdk.services.kinesis.model.PutRecordRequest
import java.time.Duration
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * 조회 이벤트 소비 경로 통합 검증 (LocalStack Kinesis + DynamoDB, 실제 바인더 구성 — KCL 모드).
 *
 * 단위 테스트가 검증하지 못하는 부분을 덮는다: Spring Cloud Stream Kinesis binder가
 * 이 프로젝트의 의존성·Boot 버전 조합에서 KCL 모드(kpl-kcl-enabled=true)로 실제 바인딩되고,
 * 배치 페이로드가 List<KinesisClientRecord>로 넘어와 이력·카운트까지 반영되며,
 * 수동 체크포인트가 지정한 이름의 KCL 리스 테이블에 실제 시퀀스 번호로 찍히는지.
 *
 * prod와 동일한 바인더 프로퍼티를 쓰고 스트림·테이블 이름과 엔드포인트만 LocalStack으로 바꾼다.
 */
@Testcontainers
@SpringBootTest(
    properties = [
        "yologram.events.subscribe.post-view.enabled=true",
        "spring.cloud.function.definition=postViewEventSubscribe",
        "spring.cloud.stream.bindings.postViewEventSubscribe-in-0.destination=test-post-view-event",
        "spring.cloud.stream.bindings.postViewEventSubscribe-in-0.group=test-group",
        "spring.cloud.stream.bindings.postViewEventSubscribe-in-0.consumer.batch-mode=true",
        "spring.cloud.stream.bindings.postViewEventSubscribe-in-0.consumer.use-native-decoding=true",
        "spring.cloud.stream.kinesis.binder.kpl-kcl-enabled=true",
        "spring.cloud.stream.kinesis.binder.auto-create-stream=false",
        "spring.cloud.stream.kinesis.bindings.postViewEventSubscribe-in-0.consumer.listener-mode=batch",
        "spring.cloud.stream.kinesis.bindings.postViewEventSubscribe-in-0.consumer.checkpoint-mode=manual",
        "spring.cloud.stream.kinesis.bindings.postViewEventSubscribe-in-0.consumer.fan-out=false",
        "spring.cloud.stream.kinesis.bindings.postViewEventSubscribe-in-0.consumer.metrics-level=NONE",
        "spring.cloud.stream.kinesis.bindings.postViewEventSubscribe-in-0.consumer.lease-table-name=test-post-view-event-lease",
        "spring.cloud.stream.kinesis.bindings.postViewEventSubscribe-in-0.consumer.shard-iterator-type=LATEST",
        // 테스트 회전 속도용 — prod는 KCL 기본값(1500ms)을 쓴다
        "spring.cloud.stream.kinesis.bindings.postViewEventSubscribe-in-0.consumer.polling-idle-time=300",
    ],
)
@ActiveProfiles("test")
class PostViewEventSubscriberIntegrationTest {

    companion object {
        const val STREAM = "test-post-view-event"

        /** prod의 lease-table-name과 같은 자리 — KCL이 이 이름으로 리스+체크포인트 테이블을 쓴다 */
        const val LEASE_TABLE = "test-post-view-event-lease"

        /** shard-iterator-type=LATEST의 기동 경쟁을 피하기 위한 워밍업 대상 (본 검증 postId와 겹치지 않게) */
        private const val WARMUP_POST_ID = 5000L

        private const val REGION = "ap-northeast-2"

        /** metrics-level=NONE 검증 — KCL이 CloudWatch를 한 번도 호출하지 않아야 한다 */
        val cloudWatchCalls = AtomicInteger(0)

        /** IAM 검증 — 리스 테이블이 이미 있으면 KCL은 CreateTable을 호출하지 않아야 한다 (prod는 생성 권한 없음) */
        val dynamoCreateTableCalls = AtomicInteger(0)

        private val consumerStarted = AtomicBoolean(false)

        @Container
        @JvmStatic
        val localstack: LocalStackContainer = LocalStackContainer(
            DockerImageName.parse("localstack/localstack:3.5")
        ).withServices(LocalStackContainer.Service.KINESIS, LocalStackContainer.Service.DYNAMODB)

        /**
         * 스트림·리스 테이블을 컨텍스트 기동 전에 만들어 둔다 — prod와 동일한 전제(terraform 선생성,
         * worker IAM에 생성 권한 없음). 리스 테이블 스키마는 KCL이 만드는 것과 같게 맞춘다
         * (leaseKey HASH / S, PAY_PER_REQUEST — DynamoDBLeaseSerializer.getKeySchema).
         * 이 상태에서 KCL이 CreateTable을 부르지 않고 부팅되는지가 IAM 정책의 관건이다.
         */
        @DynamicPropertySource
        @JvmStatic
        fun awsResources(registry: DynamicPropertyRegistry) {
            localstack.execInContainer(
                "awslocal", "kinesis", "create-stream",
                "--stream-name", STREAM, "--shard-count", "1", "--region", REGION,
            )
            localstack.execInContainer(
                "awslocal", "dynamodb", "create-table",
                "--table-name", LEASE_TABLE,
                "--attribute-definitions", "AttributeName=leaseKey,AttributeType=S",
                "--key-schema", "AttributeName=leaseKey,KeyType=HASH",
                "--billing-mode", "PAY_PER_REQUEST", "--region", REGION,
            )
            registry.add("spring.cloud.aws.region.static") { REGION }
        }
    }

    /** 바인더가 자동 생성하는 AWS 클라이언트를 LocalStack으로 돌린다 (@ConditionalOnMissingBean) */
    @TestConfiguration
    class LocalStackClients {

        private fun credentials() = StaticCredentialsProvider.create(
            AwsBasicCredentials.create(localstack.accessKey, localstack.secretKey)
        )

        @Bean
        fun kinesisAsyncClient(): KinesisAsyncClient = KinesisAsyncClient.builder()
            .endpointOverride(localstack.endpoint)
            .region(Region.of(REGION))
            .credentialsProvider(credentials())
            .build()

        @Bean
        fun dynamoDbAsyncClient(): DynamoDbAsyncClient = DynamoDbAsyncClient.builder()
            .endpointOverride(localstack.endpoint)
            .region(Region.of(REGION))
            .credentialsProvider(credentials())
            .overrideConfiguration { it.addExecutionInterceptor(CreateTableCountingInterceptor()) }
            .build()

        /**
         * KCL 모드는 CloudWatchAsyncClient 빈을 요구한다(어댑터 생성자 필수 인자).
         * metrics-level=NONE이면 NullMetricsFactory가 붙어 호출이 없어야 하므로 인터셉터로 호출 수를 센다.
         */
        @Bean
        fun cloudWatchAsyncClient(): CloudWatchAsyncClient = CloudWatchAsyncClient.builder()
            .endpointOverride(localstack.endpoint)
            .region(Region.of(REGION))
            .credentialsProvider(credentials())
            .overrideConfiguration { it.addExecutionInterceptor(CallCountingInterceptor()) }
            .build()
    }

    class CallCountingInterceptor : ExecutionInterceptor {
        override fun beforeExecution(context: Context.BeforeExecution, executionAttributes: ExecutionAttributes) {
            cloudWatchCalls.incrementAndGet()
        }
    }

    class CreateTableCountingInterceptor : ExecutionInterceptor {
        override fun beforeExecution(context: Context.BeforeExecution, executionAttributes: ExecutionAttributes) {
            if (executionAttributes.getAttribute(SdkExecutionAttribute.OPERATION_NAME) == "CreateTable") {
                dynamoCreateTableCalls.incrementAndGet()
            }
        }
    }

    @Autowired
    lateinit var kinesisAsyncClient: KinesisAsyncClient

    @Autowired
    lateinit var dynamoDbAsyncClient: DynamoDbAsyncClient

    @Autowired
    lateinit var viewRepository: TechPostViewRepository

    @Autowired
    lateinit var viewCountRepository: TechPostViewCountRepository

    private fun publish(postId: Long, uid: Long?, ip: String?, occurredAt: String) {
        val payload = """{"eventType":"POST_VIEW","section":"TECH","postId":$postId,""" +
            """"uid":${uid ?: "null"},"ip":${ip?.let { "\"$it\"" } ?: "null"},"occurredAt":"$occurredAt"}"""

        publishRaw(postId, payload)
    }

    private fun publishRaw(postId: Long, payload: String) {
        kinesisAsyncClient.putRecord(
            PutRecordRequest.builder()
                .streamName(STREAM)
                .partitionKey(postId.toString())
                .data(SdkBytes.fromUtf8String(payload))
                .build()
        ).join()
    }

    private fun viewCountOf(postId: Long): Long? = viewCountRepository.findByIdOrNull(postId)?.viewCount

    /** 리스 테이블에 기록된 샤드별 checkpoint 속성 — 최초값은 "LATEST" 센티널, 소비 후엔 시퀀스 번호 */
    private fun leaseCheckpoints(): List<String> =
        dynamoDbAsyncClient.scan { it.tableName(LEASE_TABLE) }.join()
            .items().mapNotNull { it["checkpoint"]?.s() }

    /**
     * shard-iterator-type=LATEST는 KCL이 샤드 이터레이터를 만든 시점 이후 레코드만 읽는다.
     * 컨텍스트 기동 직후 발행하면 KCL 초기화(리스 획득)와 경쟁해 유실될 수 있으므로,
     * 워밍업 레코드를 반복 발행해 소비가 실제로 시작된 것을 확인한 뒤 본 검증을 시작한다.
     * 같은 viewKey는 이력 uk가 흡수하므로 반복 발행이 카운트를 부풀리지 않는다.
     */
    private fun awaitConsumerStarted() {
        if (consumerStarted.get()) return
        await().atMost(Duration.ofMinutes(3))
            .pollInterval(Duration.ofSeconds(1))
            .ignoreExceptions()   // 스트림이 ACTIVE가 되기 전 PutRecord 실패는 재시도로 흡수
            .untilAsserted {
                publish(WARMUP_POST_ID, uid = 1, ip = null, occurredAt = "2026-08-13T00:00:00")
                assertEquals(1L, viewCountOf(WARMUP_POST_ID))
            }
        consumerStarted.set(true)
    }

    @Test
    fun `발행된 조회 이벤트가 이력과 카운트에 반영되고 중복 발행은 흡수된다`() {
        awaitConsumerStarted()
        val postId = 5100L

        // 같은 유저·같은 날 3건(새로고침 상황) + 다른 유저 1건 → 신규 조회는 2건이어야 한다
        publish(postId, uid = 12, ip = "203.0.113.7", occurredAt = "2026-08-13T00:10:00")
        publish(postId, uid = 12, ip = "203.0.113.7", occurredAt = "2026-08-13T00:10:05")
        publish(postId, uid = 12, ip = "203.0.113.7", occurredAt = "2026-08-13T01:20:00")
        publish(postId, uid = null, ip = "203.0.113.9", occurredAt = "2026-08-13T00:11:00")

        await().atMost(Duration.ofSeconds(90))
            .pollInterval(Duration.ofMillis(500))
            .untilAsserted { assertEquals(2L, viewCountOf(postId)) }

        assertEquals(
            setOf("$postId:u12:2026-08-13", "$postId:i203.0.113.9:2026-08-13"),
            viewRepository.findAll().filter { it.postId == postId }.map { it.viewKey }.toSet(),
        )
    }

    @Test
    fun `포이즌 레코드가 섞여도 소비가 멈추지 않고 정상 레코드는 반영된다`() {
        awaitConsumerStarted()
        val postId = 5200L

        // 깨진 JSON — 예외를 전파하면 이 배치가 체크포인트되지 않아 소비가 영구히 멈춘다
        publishRaw(postId, "{broken-json")
        publish(postId, uid = 77, ip = null, occurredAt = "2026-08-13T02:00:00")

        await().atMost(Duration.ofSeconds(90))
            .pollInterval(Duration.ofMillis(500))
            .untilAsserted { assertEquals(1L, viewCountOf(postId)) }
    }

    @Test
    fun `지정한 이름의 리스 테이블에 수동 체크포인트를 기록한다`() {
        awaitConsumerStarted()

        // 리스 테이블 이름 계약(선생성한 이 테이블에 리스가 잡혔는지) +
        // 체크포인트가 센티널("LATEST")이 아닌 실제 시퀀스 번호로 갱신됐는지.
        // 핸들러가 KCL 체크포인터를 인식하지 못하면 여기서 센티널에 머문다 (수동 체크포인트 미호출)
        await().atMost(Duration.ofMinutes(2))
            .pollInterval(Duration.ofSeconds(1))
            .ignoreExceptions()
            .untilAsserted {
                val checkpoints = leaseCheckpoints()
                assertEquals(1, checkpoints.size, "샤드 1개 = 리스 1건이어야 한다: $checkpoints")
                assertTrue(
                    checkpoints.single().all { it.isDigit() },
                    "체크포인트가 시퀀스 번호로 기록되지 않음 (수동 체크포인트 미반영): $checkpoints",
                )
            }
    }

    @Test
    fun `리스 테이블이 이미 있으면 CreateTable을 호출하지 않는다`() {
        awaitConsumerStarted()

        // prod IAM 결정 근거 — KCL은 DescribeTable로 존재를 먼저 확인하고 있으면 그대로 쓴다
        // (DynamoDBLeaseRefresher.createTableIfNotExists). 즉 dynamodb:CreateTable 권한이 필요 없다
        assertEquals(
            0, dynamoCreateTableCalls.get(),
            "리스 테이블이 선생성돼 있는데 CreateTable을 호출함 — prod에 생성 권한이 필요해진다",
        )
    }

    @Test
    fun `metrics-level=NONE이면 CloudWatch를 호출하지 않는다`() {
        awaitConsumerStarted()

        // 소비가 한 바퀴 이상 돈 뒤에도 CloudWatch 요청이 0건 — NullMetricsFactory가 붙었다는 간접 증거
        assertEquals(0, cloudWatchCalls.get(), "CloudWatch 호출이 발생함 — metrics-level=NONE이 적용되지 않았다")
    }
}
