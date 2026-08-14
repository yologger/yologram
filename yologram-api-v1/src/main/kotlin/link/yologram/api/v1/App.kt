package link.yologram.api.v1

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.context.properties.ConfigurationPropertiesScan
import org.springframework.boot.runApplication
import java.util.TimeZone

@SpringBootApplication
@ConfigurationPropertiesScan
class App

fun main(args: Array<String>) {
    // 기본 타임존 고정 — 컨테이너 기본(UTC)에 좌우되지 않게. worker와 같은 처리이고
    // 번장 전 서비스(bun-pms-api·bun-myhome-api·bun-video-api·각 worker)가 쓰는 방식이다.
    // 이 줄이 없어 api-v1은 UTC 벽시계로, worker는 KST 벽시계로 시각을 만들어
    // 같은 게시글의 createdAt이 9시간 어긋났다(검색 색인에서 드러났다. docs/done.md)
    TimeZone.setDefault(TimeZone.getTimeZone("Asia/Seoul"))

    runApplication<App>(*args)
}
