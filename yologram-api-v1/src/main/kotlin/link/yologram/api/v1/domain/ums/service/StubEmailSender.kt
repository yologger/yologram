package link.yologram.api.v1.domain.ums.service

import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Component

private val logger = KotlinLogging.logger {}

@Component
@Profile("!prod")
class StubEmailSender : EmailSender {

    override fun sendVerificationCode(to: String, code: String) {
        logger.info { "[StubEmailSender] to=$to, code=$code" }
    }

    override fun sendUserPasswordResetCode(to: String, code: String) {
        logger.info { "[StubEmailSender] 비밀번호 재설정 to=$to, code=$code" }
    }
}
