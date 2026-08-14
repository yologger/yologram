package link.yologram.api.v1.domain.ums.service

import link.yologram.api.v1.config.ses.SesProperties
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.any
import org.mockito.kotlin.argThat
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import software.amazon.awssdk.services.ses.SesClient
import software.amazon.awssdk.services.ses.model.SendEmailRequest
import software.amazon.awssdk.services.ses.model.SendEmailResponse
import software.amazon.awssdk.services.ses.model.SesException

@ExtendWith(MockitoExtension::class)
class SesEmailSenderTest {

    @Mock
    lateinit var sesClient: SesClient

    @Mock
    lateinit var sesProperties: SesProperties

    @InjectMocks
    lateinit var sesEmailSender: SesEmailSender

    @Nested
    inner class 발송_성공 {

        @Test
        fun `SES로 이메일을 발송한다`() {
            whenever(sesProperties.fromAddress).thenReturn("no-reply@yologram.link")
            whenever(sesClient.sendEmail(any<SendEmailRequest>()))
                .thenReturn(SendEmailResponse.builder().build())

            sesEmailSender.sendVerificationCode("test@yologram.link", "123456")

            verify(sesClient).sendEmail(argThat<SendEmailRequest> {
                source() == "no-reply@yologram.link"
                    && destination().toAddresses().contains("test@yologram.link")
                    && message().subject().data() == "[yologram] 이메일 인증 코드"
                    && message().body().html() != null
                    && message().body().html().data().contains("123456")
            })
        }

        @Test
        fun `HTML 본문에 인증 코드가 포함된다`() {
            whenever(sesProperties.fromAddress).thenReturn("no-reply@yologram.link")
            whenever(sesClient.sendEmail(any<SendEmailRequest>()))
                .thenReturn(SendEmailResponse.builder().build())

            sesEmailSender.sendVerificationCode("test@yologram.link", "654321")

            verify(sesClient).sendEmail(argThat<SendEmailRequest> {
                message().body().html().data().contains("654321")
            })
        }
    }

    @Nested
    inner class 발송_실패 {

        @Test
        fun `SES 예외 발생 시 그대로 던진다`() {
            whenever(sesProperties.fromAddress).thenReturn("no-reply@yologram.link")
            whenever(sesClient.sendEmail(any<SendEmailRequest>()))
                .thenThrow(SesException.builder().message("MessageRejected").build())

            assertThrows<SesException> {
                sesEmailSender.sendVerificationCode("test@yologram.link", "123456")
            }
        }
    }
}
