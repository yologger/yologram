package link.yologram.api.v1.config.async

import org.springframework.boot.autoconfigure.task.TaskExecutionAutoConfiguration.APPLICATION_TASK_EXECUTOR_BEAN_NAME
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.scheduling.annotation.AsyncConfigurer
import org.springframework.scheduling.annotation.EnableAsync
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor
import java.util.concurrent.Executor

/**
 * @Async 실행용 스레드 풀 (번장 bun-pay3-batch-job AsyncConfig 미러 — 용도별 분리는 우리 쪽 변형).
 *
 * 기본 풀까지 여기서 직접 정의한다. Boot의 applicationTaskExecutor 자동구성은
 * @ConditionalOnMissingBean(Executor)라서 Executor 빈을 하나라도 만들면 통째로 백오프한다 —
 * 그러면 기본 풀이 사라진 채 SimpleAsyncTaskExecutor(호출마다 새 스레드)로 조용히 폴백하므로,
 * 자동구성에 의존하지 않고 두 풀을 나란히 두어 무엇이 어디에 쓰이는지 코드에서 드러나게 했다.
 *
 * 용도마다 풀을 나누는 이유: 하나를 공유하면 오래 걸리는 작업(풀 인덱싱 발행)이 스레드를 다 잡았을 때
 * 다른 비동기 작업이 큐에서 대기한다.
 *
 * 풀에 올리는 작업은 "결과를 아무도 기다리지 않는" 것만이다 —
 * @Async 메서드의 예외는 호출자에게 전달되지 않기 때문이다(원본도 슬랙 알림에만 쓴다).
 * 큐는 인메모리라 인스턴스가 죽으면 대기 중인 작업이 사라진다 — 유실되면 곤란한 작업은 SQS로 보낼 것.
 *
 * 풀 크기는 원본(core 10 / max 100 / queue 1000)보다 줄였다 — api-v1은 0.25vCPU라
 * 스레드를 늘려봐야 요청 처리 스레드와 CPU를 다투기만 한다
 * (worker가 0.25vCPU에서 KCL 이벤트 루프를 굶겨 소비가 멈춘 선례가 있다. docs/done.md 사고 ③).
 */
@Configuration
@EnableAsync
class AsyncConfig : AsyncConfigurer {

    /** 기본 풀 — 이름 없는 @Async, Spring MVC 비동기 요청 처리, JPA 부트스트랩이 함께 쓴다 */
    @Bean(APPLICATION_TASK_EXECUTOR_BEAN_NAME)
    fun applicationTaskExecutor(): ThreadPoolTaskExecutor = ThreadPoolTaskExecutor().apply {
        corePoolSize = 2
        maxPoolSize = 4
        queueCapacity = 100
        setThreadNamePrefix("appExecutor-")
        setWaitForTasksToCompleteOnShutdown(true)
        setAwaitTerminationSeconds(60)
        initialize()
    }

    /**
     * SQS 발행 전용 — @Async("sqsTaskExecutor")로 이름을 지정할 때만 쓴다.
     * defaultCandidate = false라 타입 기반 자동 주입 후보에서 빠진다(기본 풀과 섞이지 않게).
     */
    @Bean("sqsTaskExecutor", defaultCandidate = false)
    fun sqsTaskExecutor(): ThreadPoolTaskExecutor = ThreadPoolTaskExecutor().apply {
        corePoolSize = 2
        maxPoolSize = 4
        queueCapacity = 100
        setThreadNamePrefix("sqsExecutor-")
        // 종료 시점에 진행 중인 작업을 최대 60초 기다린다 (그 사이 발행분까지는 살린다)
        setWaitForTasksToCompleteOnShutdown(true)
        setAwaitTerminationSeconds(60)
        initialize()
    }

    /** 이름 없는 @Async가 쓸 풀 — 명시하지 않으면 Executor 빈이 둘이라 SimpleAsyncTaskExecutor로 폴백한다 */
    override fun getAsyncExecutor(): Executor = applicationTaskExecutor()
}
