package link.yologram.api.v1.domain.ums.service

import io.github.oshai.kotlinlogging.KotlinLogging
import link.yologram.api.v1.config.SesProperties
import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Component
import software.amazon.awssdk.services.ses.SesClient
import software.amazon.awssdk.services.ses.model.Body
import software.amazon.awssdk.services.ses.model.Content
import software.amazon.awssdk.services.ses.model.Destination
import software.amazon.awssdk.services.ses.model.Message
import software.amazon.awssdk.services.ses.model.SendEmailRequest

private val logger = KotlinLogging.logger {}

@Component
@Profile("prod")
class SesEmailSender(
    private val sesClient: SesClient,
    private val sesProperties: SesProperties,
) : EmailSender {

    override fun sendVerificationCode(to: String, code: String) {
        val html = """
            <!DOCTYPE html>
            <html>
            <head><meta charset="UTF-8"></head>
            <body style="margin:0;padding:0;background-color:#f4f4f5;font-family:-apple-system,BlinkMacSystemFont,'Segoe UI',Roboto,sans-serif;">
              <table width="100%" cellpadding="0" cellspacing="0" style="padding:40px 20px;">
                <tr><td align="center">
                  <table width="420" cellpadding="0" cellspacing="0" style="background-color:#ffffff;border-radius:12px;overflow:hidden;">
                    <tr>
                      <td style="padding:32px 36px 0;text-align:center;">
                        <h1 style="margin:0;font-size:24px;font-weight:700;color:#18181b;">yologram</h1>
                      </td>
                    </tr>
                    <tr>
                      <td style="padding:28px 36px 0;text-align:center;">
                        <p style="margin:0;font-size:16px;color:#3f3f46;">이메일 인증 코드</p>
                        <p style="margin:8px 0 0;font-size:14px;color:#71717a;">아래 인증 코드를 입력해 주세요.</p>
                      </td>
                    </tr>
                    <tr>
                      <td style="padding:24px 36px;">
                        <div style="background-color:#f4f4f5;border-radius:8px;padding:20px;text-align:center;">
                          <span style="font-size:32px;font-weight:700;letter-spacing:8px;color:#18181b;">$code</span>
                        </div>
                      </td>
                    </tr>
                    <tr>
                      <td style="padding:0 36px 32px;text-align:center;">
                        <p style="margin:0;font-size:13px;color:#a1a1aa;">이 코드는 5분간 유효합니다.</p>
                      </td>
                    </tr>
                    <tr>
                      <td style="padding:16px 36px;border-top:1px solid #e4e4e7;text-align:center;">
                        <p style="margin:0;font-size:12px;color:#a1a1aa;">&copy; 2026 yologram</p>
                      </td>
                    </tr>
                  </table>
                </td></tr>
              </table>
            </body>
            </html>
        """.trimIndent()

        val request = SendEmailRequest.builder()
            .source(sesProperties.fromAddress)
            .destination(Destination.builder().toAddresses(to).build())
            .message(
                Message.builder()
                    .subject(Content.builder().data("[yologram] 이메일 인증 코드").charset("UTF-8").build())
                    .body(
                        Body.builder()
                            .html(Content.builder().data(html).charset("UTF-8").build())
                            .build()
                    )
                    .build()
            )
            .build()

        try {
            sesClient.sendEmail(request)
            logger.info { "[SesEmailSender] to=$to" }
        } catch (e: Exception) {
            logger.error(e) { "[SesEmailSender] 발송 실패: to=$to" }
            throw e
        }
    }
}
