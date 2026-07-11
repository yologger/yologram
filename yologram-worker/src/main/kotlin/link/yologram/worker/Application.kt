package link.yologram.worker

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import java.util.TimeZone

@SpringBootApplication
class Application

fun main(args: Array<String>) {
    // JVM 레벨 초기화 (Spring 컨텍스트보다 먼저)
    init()

    runApplication<Application>(*args)
}

fun init() {
    // 기본 타임존 고정 — 스케줄·수집 시각이 컨테이너 기본(UTC)에 좌우되지 않게
    TimeZone.setDefault(TimeZone.getTimeZone("Asia/Seoul"))

    /**
     * JVM DNS 캐시 TTL 설정 — AWS 엔드포인트의 IP 변경을 따라가도록 무한 캐시를 방지
     * @see <a href="https://docs.aws.amazon.com/sdk-for-java/v1/developer-guide/java-dg-jvm-ttl.html">AWS Guide</a>
     */
    java.security.Security.setProperty("networkaddress.cache.ttl", "60")
    java.security.Security.setProperty("networkaddress.cache.negative.ttl", "10")
}
